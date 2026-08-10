package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import za.co.tut.stokvelchain.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "loan_id", updatable = false, nullable = false)
    private UUID loanId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Loan amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Min(1) @Max(24)
    @Column(name = "repayment_months", nullable = false)
    private int repaymentMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    /**
     * Credit eligibility probability (0.0 – 1.0) returned by the Flask ML sidecar.
     * Displayed to the Group Admin on the loan review screen.
     */
    @Column(name = "ai_score", precision = 5, scale = 4)
    private BigDecimal aiScore;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /** Timestamp of the Admin's approval or rejection decision. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Ethereum tx hash from disburseLoan() — null until approved and confirmed. */
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_loan_member"))
    private MemberEntity member;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanRepaymentEntity> repayments = new ArrayList<>();
}
