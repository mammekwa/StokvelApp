package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutPreviewResponse {
    UUID recipientMemberId;
    String recipientName;
    Integer recipientPosition;

    /** Group balance: contributions + repayments − payouts − disbursed loans. */
    BigDecimal availableAmount;

    LocalDate dueDate;
    /** True when paying out before the due date for the group's payout cycle. */
    boolean early;

    /** Active members with no confirmed contribution since the last payout (warning only). */
    List<String> missingContributors;

    /** Set when a previous payout is still unconfirmed on-chain — it must be retried first. */
    UUID pendingPayoutId;

    List<PayoutRotationEntry> rotation;
}
