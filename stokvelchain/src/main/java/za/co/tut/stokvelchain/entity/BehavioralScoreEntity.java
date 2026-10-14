package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import za.co.tut.stokvelchain.enums.ModelRecommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "behavioral_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehavioralScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "score_id", updatable = false, nullable = false)
    private UUID scoreId;

    /** Ratio of on-time contributions to expected contributions (0.0 – 1.0). */
    @Column(name = "contribution_freq", precision = 5, scale = 4)
    private BigDecimal contributionFreq;

    /** Ratio of payout cycles complied with (0.0 – 1.0). */
    @Column(name = "payout_compliance", precision = 5, scale = 4)
    private BigDecimal payoutCompliance;

    /** Ratio of loan repayments made on time (0.0 – 1.0). */
    @Column(name = "repayment_ratio", precision = 5, scale = 4)
    private BigDecimal repaymentRatio;

    /** Number of months the member has been active in their current group. */
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    /** Outstanding amount relative to the member's capacity (0.0 – 1.0). */
    @Column(name = "credit_amount_normalised", precision = 5, scale = 4)
    private BigDecimal creditAmountNormalised;

    /** Member's age at scoring time, as sent to the ML model. */
    @Column(name = "age")
    private Integer age;

    /** Decision threshold the model used at scoring time. */
    @Column(name = "threshold", precision = 5, scale = 4)
    private BigDecimal threshold;

    /** Model's verdict: APPROVE if aiScore >= threshold, otherwise DECLINE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", length = 20)
    private ModelRecommendation recommendation;

    /** Version of the deployed model that produced this score. */
    @Column(name = "model_version", length = 40)
    private String modelVersion;

    /** SHAP reasons returned by the ML sidecar, stored as raw JSON for audit. */
    @Column(name = "reasons_json", columnDefinition = "TEXT")
    private String reasonsJson;

    /**
     * Credit eligibility probability (0.0 – 1.0) returned by the ML model.
     * Multiplied by 100 for display on the Admin dashboard (e.g. 0.78 → 78/100).
     */
    @Column(name = "ai_score", precision = 5, scale = 4)
    private BigDecimal aiScore;

    @CreationTimestamp
    @Column(name = "scored_at", nullable = false, updatable = false)
    private LocalDateTime scoredAt;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_score_member"))
    private MemberEntity member;

    /** Loan request this score was produced for — null for ad-hoc scoring. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id",
            foreignKey = @ForeignKey(name = "fk_score_loan"))
    private LoanEntity loan;
}
