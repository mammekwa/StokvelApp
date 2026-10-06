package za.co.tut.stokvelchain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.PayoutCycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CreateGroupResponse {
    UUID groupId;
    String groupName;
    PayoutCycle payoutCycle;
    BigDecimal contributionAmount;
    ContributionFrequency contributionFrequency;
    @JsonProperty("isAdmin")
    boolean isAdmin;
    long activeMemberCount;
    String inviteCode;
    String inviteLink;
    LocalDateTime createdAt;
}
