package za.co.tut.stokvelchain.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InterestRatePolicy {

    @Value("${loans.interest.low-risk-min-score:0.80}")
    private BigDecimal lowRiskMinScore;

    @Value("${loans.interest.medium-risk-min-score:0.70}")
    private BigDecimal mediumRiskMinScore;

    @Value("${loans.interest.low-risk-rate:0.05}")
    private BigDecimal lowRiskRate;

    @Value("${loans.interest.medium-risk-rate:0.10}")
    private BigDecimal mediumRiskRate;

    @Value("${loans.interest.elevated-risk-rate:0.15}")
    private BigDecimal elevatedRiskRate;

    /** Applies when the admin approved a loan the model recommended declining. */
    @Value("${loans.interest.high-risk-rate:0.20}")
    private BigDecimal highRiskRate;

    /**
     * @param aiScore   approval probability from the model (0.0 – 1.0)
     * @param threshold the model's decision threshold at scoring time
     * @return flat interest rate as a fraction, e.g. 0.10 for 10%
     */
    public BigDecimal rateFor(BigDecimal aiScore, BigDecimal threshold) {
        if (aiScore == null) {
            return highRiskRate;
        }
        if (aiScore.compareTo(lowRiskMinScore) >= 0) {
            return lowRiskRate;
        }
        if (aiScore.compareTo(mediumRiskMinScore) >= 0) {
            return mediumRiskRate;
        }
        if (threshold != null && aiScore.compareTo(threshold) >= 0) {
            return elevatedRiskRate;
        }
        return highRiskRate;
    }
}