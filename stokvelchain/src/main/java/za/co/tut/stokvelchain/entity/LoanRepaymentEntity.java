package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "repayment_id", updatable = false, nullable = false)
    private UUID repaymentId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Repayment amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** Ethereum tx hash from recordRepayment() — null until confirmed. */
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "blockchain_confirmed", nullable = false)
    @Builder.Default
    private boolean blockchainConfirmed = false;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_repayment_loan"))
    private LoanEntity loan;
}
