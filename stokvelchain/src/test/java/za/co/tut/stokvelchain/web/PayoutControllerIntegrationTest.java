package za.co.tut.stokvelchain.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.tut.stokvelchain.controlller.PayoutController;
import za.co.tut.stokvelchain.dto.response.PayoutPreviewResponse;
import za.co.tut.stokvelchain.dto.response.PayoutResponse;
import za.co.tut.stokvelchain.dto.response.PayoutRotationEntry;
import za.co.tut.stokvelchain.exception.GlobalExceptionHandler;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.services.PayoutService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayoutControllerIntegrationTest {

    private final UUID groupId = UUID.randomUUID();
    private PayoutService payoutService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        payoutService = mock(PayoutService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PayoutController(payoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /payouts/next returns the next recipient, the balance and the rotation")
    void previewReturnsNextRecipient() throws Exception {
        UUID lerato = UUID.randomUUID();
        when(payoutService.previewNextPayout(groupId)).thenReturn(PayoutPreviewResponse.builder()
                .recipientMemberId(lerato)
                .recipientName("Lerato Dlamini")
                .recipientPosition(2)
                .availableAmount(new BigDecimal("1600.00"))
                .missingContributors(List.of("Sipho Nkosi"))
                .rotation(List.of(
                        PayoutRotationEntry.builder().memberId(UUID.randomUUID()).memberName("Thabo Mokoena").payoutPosition(1).next(false).build(),
                        PayoutRotationEntry.builder().memberId(lerato).memberName("Lerato Dlamini").payoutPosition(2).next(true).build()))
                .build());

        mvc.perform(get("/api/v1/groups/{groupId}/payouts/next", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Lerato Dlamini"))
                .andExpect(jsonPath("$.recipientPosition").value(2))
                .andExpect(jsonPath("$.availableAmount").isNumber())
                .andExpect(jsonPath("$.missingContributors[0]").value("Sipho Nkosi"))
                .andExpect(jsonPath("$.rotation", hasSize(2)))
                .andExpect(jsonPath("$.rotation[1].next").value(true));
    }

    @Test
    @DisplayName("POST /payouts returns 201 with the blockchain transaction hash")
    void processReturnsCreatedWithTxHash() throws Exception {
        when(payoutService.processNextPayout(groupId)).thenReturn(PayoutResponse.builder()
                .payoutId(UUID.randomUUID())
                .recipientName("Lerato Dlamini")
                .recipientPosition(2)
                .amount(new BigDecimal("1600.00"))
                .txHash("0x" + "ab".repeat(32))
                .blockchainConfirmed(true)
                .build());

        mvc.perform(post("/api/v1/groups/{groupId}/payouts", groupId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientName").value("Lerato Dlamini"))
                .andExpect(jsonPath("$.blockchainConfirmed").value(true))
                .andExpect(jsonPath("$.txHash").value("0x" + "ab".repeat(32)));
    }

    @Test
    @DisplayName("GET /payouts returns the payout history")
    void historyReturnsList() throws Exception {
        when(payoutService.getGroupPayouts(groupId)).thenReturn(List.of(
                PayoutResponse.builder().payoutId(UUID.randomUUID()).recipientName("Thabo Mokoena").blockchainConfirmed(true).build()));

        mvc.perform(get("/api/v1/groups/{groupId}/payouts", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipientName").value("Thabo Mokoena"));
    }

    @Test
    @DisplayName("An unknown group returns 404")
    void unknownGroupReturnsNotFound() throws Exception {
        when(payoutService.previewNextPayout(groupId)).thenThrow(new ResourceNotFoundException("Group not found: " + groupId));

        mvc.perform(get("/api/v1/groups/{groupId}/payouts/next", groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group not found: " + groupId));
    }
}

