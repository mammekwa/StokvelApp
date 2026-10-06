package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.PayoutCycle;

import java.math.BigDecimal;

/** What someone sees when they open a join link. Deliberately minimal: this endpoint is public. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPreviewResponse {
    String groupName;
    BigDecimal contributionAmount;
    ContributionFrequency contributionFrequency;
    PayoutCycle payoutCycle;
    long activeMemberCount;
}
