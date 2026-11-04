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
public class MemberArrearsEntry {
    UUID memberId;
    String memberName;
    Integer payoutPosition;
    /** Contributions due in the period (from when the member became active). */
    int contributionsDue;
    BigDecimal expected;
    /** Confirmed on the blockchain. */
    BigDecimal paid;
    /** Logged but not yet confirmed on the blockchain; not counted as paid. */
    BigDecimal unconfirmed;
    /** expected − paid, never below zero. */
    BigDecimal arrears;
    int contributionCount;
    LocalDate lastContributionDate;
}