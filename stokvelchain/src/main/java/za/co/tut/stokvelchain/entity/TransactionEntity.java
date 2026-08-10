package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import za.co.tut.stokvelchain.enums.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tx_id", updatable = false, nullable = false)
    private UUID txId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    /**
     * Name of the source table (e.g. "contributions", "loans", "payouts").
     * Used alongside sourceRecordId to trace back to the originating domain record.
     */
    @NotBlank
    @Column(name = "source_table", nullable = false, length = 50)
    private String sourceTable;

    /** UUID of the record in sourceTable that triggered this transaction. */
    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Ethereum transaction hash — the permanent on-chain identifier.
     * Members and Admins can verify this on the Sepolia Etherscan explorer.
     */
    @NotBlank
    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    /** Block number on the Ethereum chain where this transaction was mined. */
    @Column(name = "block_number")
    private Long blockNumber;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    //Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_member"))
    private MemberEntity member;
}
