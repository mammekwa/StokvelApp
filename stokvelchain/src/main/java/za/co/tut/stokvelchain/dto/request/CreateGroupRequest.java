package za.co.tut.stokvelchain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.PayoutCycle;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
public class CreateGroupRequest {
    @NotBlank
    @Size(min = 3, max = 100) String groupName;
    @NotNull
    PayoutCycle payoutCycle;
    @NotNull @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    BigDecimal contributionAmount;
    @NotNull
    ContributionFrequency contributionFrequency;
}
