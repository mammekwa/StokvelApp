package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_national_id", columnNames = "national_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id", updatable = false, nullable = false)
    private UUID memberId;

    // ── Core identity ─────────────────────────────────────────────────────────
    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^[0-9]{13}$", message = "South African ID must be exactly 13 digits")
    @Column(name = "national_id", nullable = false, length = 13)
    private String nationalId;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;

    /**
     * When true, the member cannot submit new loan applications.
     * Set automatically by the smart contract when 2 consecutive
     * contributions are missed, and cleared on arrears settlement.
     */
    @Column(name = "loan_restricted", nullable = false)
    @Builder.Default
    private boolean loanRestricted = false;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    //Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_user"))
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_group"))
    private StokvelGroupEntity group;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ContributionEntity> contributions = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanEntity> loans = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BehavioralScoreEntity> behavioralScores = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();
}
