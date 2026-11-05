package za.co.tut.stokvelchain.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.tut.stokvelchain.controlller.LoanController;
import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.dto.response.LoanResponse;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.enums.ModelRecommendation;
import za.co.tut.stokvelchain.exception.GlobalExceptionHandler;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
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
    @DisplayName("A user outside the group gets 404 with a clear message")
    void nonMemberGetsNotFound() throws Exception {
        when(loanService.getMyLoans(groupId)).thenThrow(new ResourceNotFoundException("You are not a member of this group"));

        mvc.perform(get("/api/v1/groups/{groupId}/loans/me", groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("You are not a member of this group"));
    }
}
