package za.co.tut.stokvelchain.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import za.co.tut.stokvelchain.controlller.LoanController;
import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.dto.request.LoanDecisionRequest;
import za.co.tut.stokvelchain.dto.response.LoanResponse;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.enums.ModelRecommendation;
import za.co.tut.stokvelchain.exception.*;
import za.co.tut.stokvelchain.services.LoanService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerIntegrationTest {

    private final UUID groupId = UUID.randomUUID();
    private LoanService loanService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        loanService = mock(LoanService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LoanController(loanService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /loans with a valid application returns 201 and the AI score")
    void applyReturnsCreatedWithScore() throws Exception {
        when(loanService.applyForLoan(eq(groupId), any(LoanApplicationRequest.class))).thenReturn(LoanResponse.builder()
                .loanId(UUID.randomUUID())
                .memberName("Ayanda Zulu")
                .amount(new BigDecimal("1500.00"))
                .repaymentMonths(6)
                .status(LoanStatus.PENDING)
                .aiScore(new BigDecimal("0.7100"))
                .threshold(new BigDecimal("0.557"))
                .recommendation(ModelRecommendation.APPROVE)
                .build());

        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1500, \"repaymentMonths\": 6}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberName").value("Ayanda Zulu"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.recommendation").value("APPROVE"))
                .andExpect(jsonPath("$.aiScore").isNumber())
                .andExpect(jsonPath("$.threshold").isNumber());
    }

    @Test
    @DisplayName("POST /loans with amount 0 returns 400 and names the invalid field")
    void zeroAmountIsRejected() throws Exception {
        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0, \"repaymentMonths\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing or invalid fields"))
                .andExpect(jsonPath("$.details[0]").value(containsString("amount")));
        verify(loanService, never()).applyForLoan(any(), any());
    }

    @Test
    @DisplayName("POST /loans with a 30-month term returns 400 (maximum is 24)")
    void termAboveMaximumIsRejected() throws Exception {
        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000, \"repaymentMonths\": 30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("repaymentMonths")));
    }

    @Test
    @DisplayName("GET /loans/me returns 200 with the member's loans")
    void myLoansReturnsList() throws Exception {
        when(loanService.getMyLoans(groupId)).thenReturn(List.of(
                LoanResponse.builder().loanId(UUID.randomUUID()).status(LoanStatus.APPROVED).txHash("0xabc").build(),
                LoanResponse.builder().loanId(UUID.randomUUID()).status(LoanStatus.REPAID).build()));

        mvc.perform(get("/api/v1/groups/{groupId}/loans/me", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].txHash").value("0xabc"));
    }

    @Test
    @DisplayName("Not enough contributions: 422 with the rule explained, so the member knows what to do")
    void ineligibleApplicationReturns422() throws Exception {
        when(loanService.applyForLoan(eq(groupId), any(LoanApplicationRequest.class)))
                .thenThrow(new LoanEligibilityException("At least 3 confirmed contributions are required (you have 1)"));

        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000, \"repaymentMonths\": 6}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("At least 3 confirmed contributions are required (you have 1)"));
    }

    @Test
    @DisplayName("Scoring service down: 503 telling the member nothing was saved and to try again")
    void scoringServiceDownReturns503() throws Exception {
        when(loanService.applyForLoan(eq(groupId), any(LoanApplicationRequest.class)))
                .thenThrow(new CreditScoringException("Connection refused"));

        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000, \"repaymentMonths\": 6}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("try again")));
    }

    @Test
    @DisplayName("Override without a reason: 400 with the requirement, so the admin can correct it")
    void overrideWithoutReasonReturns400() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.decideLoan(eq(groupId), eq(loanId), any(LoanDecisionRequest.class)))
                .thenThrow(new LoanDecisionException("Overriding the credit model requires a category and a reason of at least 20 characters"));

        mvc.perform(post("/api/v1/groups/{groupId}/loans/{loanId}/decision", groupId, loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("reason")));
    }

    @Test
    @DisplayName("Admin deciding their own loan: 403 with the reason")
    void selfDecisionReturns403() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.decideLoan(eq(groupId), eq(loanId), any(LoanDecisionRequest.class)))
                .thenThrow(new AccessDeniedException("You can't decide your own loan application; another group member must review it"));

        mvc.perform(post("/api/v1/groups/{groupId}/loans/{loanId}/decision", groupId, loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("another group member")));
    }

    @Test
    @DisplayName("Approved but the blockchain write failed: 503 with retry instructions")
    void failedDisbursementReturns503() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.retryDisbursement(groupId, loanId))
                .thenThrow(new BlockchainOperationException("Loan approved but the disbursement could not be recorded on-chain; retry the disbursement",
                        new RuntimeException("RPC timeout")));

        mvc.perform(post("/api/v1/groups/{groupId}/loans/{loanId}/disbursement/retry", groupId, loanId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("retry")));
    }

    @Test
    @DisplayName("Malformed JSON: 400 with a readable message instead of 500")
    void malformedBodyReturns400() throws Exception {
        mvc.perform(post("/api/v1/groups/{groupId}/loans", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": \"lots\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or not in the expected format"));
    }

    @Test
    @DisplayName("Invalid group id in the URL: 400 naming the bad value")
    void invalidGroupIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/groups/{groupId}/loans/me", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'groupId'"));
    }

    @Test
    @DisplayName("Errors with an explicit status keep it (409 instead of 500)")
    void responseStatusExceptionKeepsItsStatus() throws Exception {
        when(loanService.getMyLoans(groupId))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group"));

        mvc.perform(get("/api/v1/groups/{groupId}/loans/me", groupId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You are already a member of this group"));
    }

    @Test
    @DisplayName("A user outside the group gets 404 with a clear message")
    void nonMemberGetsNotFound() throws Exception {
        when(loanService.getMyLoans(groupId)).thenThrow(new ResourceNotFoundException("You are not a member of this group"));

        mvc.perform(get("/api/v1/groups/{groupId}/loans/me", groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("You are not a member of this group"));
    }
}
