package za.co.tut.stokvelchain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringRequest {
    @JsonProperty("contribution_frequency_score")
    BigDecimal contributionFrequencyScore;

    @JsonProperty("payout_compliance_rate")
    BigDecimal payoutComplianceRate;

    @JsonProperty("loan_repayment_ratio")
    BigDecimal loanRepaymentRatio;

    @JsonProperty("credit_amount_normalised")
    BigDecimal creditAmountNormalised;

    @JsonProperty("age")
    int age;
}
