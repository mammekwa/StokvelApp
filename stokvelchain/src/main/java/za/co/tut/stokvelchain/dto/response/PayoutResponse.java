package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {
    UUID payoutId;
    UUID recipientMemberId;
    String recipientName;
    Integer recipientPosition;
    BigDecimal amount;
    LocalDate scheduledDate;
    LocalDateTime processedAt;
    String txHash;
    boolean blockchainConfirmed;
}
