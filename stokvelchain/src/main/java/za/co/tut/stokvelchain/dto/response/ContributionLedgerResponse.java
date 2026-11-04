package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.ContributionFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionLedgerResponse {
    LocalDate from;
    LocalDate to;
    BigDecimal contributionAmount;
    ContributionFrequency contributionFrequency;
    BigDecimal totalExpected;
    BigDecimal totalPaid;
    BigDecimal totalArrears;
    List<MemberArrearsEntry> members;
    List<ContributionLedgerEntry> contributions;
}

