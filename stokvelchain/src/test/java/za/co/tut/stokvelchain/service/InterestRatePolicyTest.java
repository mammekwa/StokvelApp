package za.co.tut.stokvelchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.tut.stokvelchain.services.impl.InterestRatePolicy;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InterestRatePolicyTest {

    private static final BigDecimal THRESHOLD = new BigDecimal("0.557");

    private InterestRatePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new InterestRatePolicy();
        // Same defaults as the @Value annotations (no Spring context in a unit test).
        ReflectionTestUtils.setField(policy, "lowRiskMinScore", new BigDecimal("0.80"));
        ReflectionTestUtils.setField(policy, "mediumRiskMinScore", new BigDecimal("0.70"));
        ReflectionTestUtils.setField(policy, "lowRiskRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(policy, "mediumRiskRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(policy, "elevatedRiskRate", new BigDecimal("0.15"));
        ReflectionTestUtils.setField(policy, "highRiskRate", new BigDecimal("0.20"));
    }

    @ParameterizedTest(name = "score {0} -> {1} interest")
    @CsvSource({
            "0.95,  0.05",
            "0.80,  0.05",
            "0.79,  0.10",
            "0.70,  0.10",
            "0.69,  0.15",
            "0.557, 0.15",
            "0.556, 0.20",
            "0.30,  0.20"
    })
    @DisplayName("Higher credit scores get lower interest")
    void rateFollowsTheScoreBands(String score, String expectedRate) {
        assertThat(policy.rateFor(new BigDecimal(score), THRESHOLD)).isEqualByComparingTo(expectedRate);
    }

    @Test
    @DisplayName("A loan with no score is priced as high risk")
    void missingScoreIsHighRisk() {
        assertThat(policy.rateFor(null, THRESHOLD)).isEqualByComparingTo("0.20");
    }

    @Test
    @DisplayName("Without a threshold, scores below 0.70 are priced as high risk")
    void missingThresholdIsHighRisk() {
        assertThat(policy.rateFor(new BigDecimal("0.65"), null)).isEqualByComparingTo("0.20");
    }
}