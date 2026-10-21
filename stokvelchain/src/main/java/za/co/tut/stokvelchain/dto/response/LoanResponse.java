package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.enums.ModelRecommendation;
import za.co.tut.stokvelchain.enums.OverrideReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    UUID loanId;
    String memberName;
    BigDecimal amount;
    int repaymentMonths;
    LoanStatus status;

    // Repayment terms and progress
    BigDecimal interestRate;
    BigDecimal totalRepayable;
    BigDecimal totalRepaid;
    BigDecimal outstanding;
    BigDecimal instalmentAmount;

    // Credit model output (explainability)
    BigDecimal aiScore;
    BigDecimal threshold;
    ModelRecommendation recommendation;
    List<ScoreReason> reasons;

    // Admin decision
    boolean overridden;
    OverrideReason overrideCategory;
    String overrideReason;

    LocalDateTime requestedAt;
    LocalDateTime resolvedAt;
    String txHash;
}