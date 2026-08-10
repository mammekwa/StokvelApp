package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contribution_id", updatable = false, nullable = false)
    private UUID contributionId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Size(max = 100)
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    /**
     * Ethereum transaction hash returned by the smart contract call.
     * Null if the blockchain transaction has not yet been confirmed.
     */
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    /**
     * True if the blockchain transaction was confirmed on-chain.
     * False if the DB write succeeded but the contract call failed (unconfirmed state).
     */
    @Column(name = "blockchain_confirmed", nullable = false)
    @Builder.Default
    private boolean blockchainConfirmed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_contribution_member"))
    private MemberEntity member;
}
