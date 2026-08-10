package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.PayoutCycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stokvel_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StokvelGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_id", updatable = false, nullable = false)
    private UUID groupId;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    /**
     * The User who administers this group.
     * Stored as a FK rather than a full User relationship
     * to keep group creation independent of member registration order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_group_admin_user"))
    private UserEntity adminUser;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_cycle", nullable = false, length = 20)
    private PayoutCycle payoutCycle;

    @NotNull
    @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    @Column(name = "contribution_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal contributionAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_frequency", nullable = false, length = 20)
    private ContributionFrequency contributionFrequency;

    /**
     * Ethereum address of the deployed smart contract for this group.
     * Populated after the contract is deployed to the Sepolia testnet.
     */
    @Column(name = "smart_contract_address", length = 42)
    private String smartContractAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Relationships
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PayoutEntity> payouts = new ArrayList<>();
}
