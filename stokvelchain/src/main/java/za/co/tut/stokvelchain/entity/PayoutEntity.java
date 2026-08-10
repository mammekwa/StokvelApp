package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payout_id", updatable = false, nullable = false)
    private UUID payoutId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Payout amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    /** Timestamp when the Admin triggered the payout and the contract call succeeded. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** Ethereum tx hash from processPayout() — null until confirmed. */
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "blockchain_confirmed", nullable = false)
    @Builder.Default
    private boolean blockchainConfirmed = false;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payout_group"))
    private StokvelGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payout_member"))
    private MemberEntity member;
}
