package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionLedgerEntry {
    UUID contributionId;
    UUID memberId;
    String memberName;
    BigDecimal amount;
    LocalDate contributionDate;
    String paymentReference;
    String txHash;
    boolean blockchainConfirmed;
}
