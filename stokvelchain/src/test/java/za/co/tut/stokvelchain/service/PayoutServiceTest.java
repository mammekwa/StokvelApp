package za.co.tut.stokvelchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import za.co.tut.stokvelchain.dto.response.PayoutPreviewResponse;
import za.co.tut.stokvelchain.dto.response.PayoutResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.PayoutEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.MemberStatus;
import za.co.tut.stokvelchain.enums.PayoutCycle;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.exception.PayoutException;
import za.co.tut.stokvelchain.repository.*;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.NotificationService;
import za.co.tut.stokvelchain.services.impl.PayoutServiceImpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ContributionRepository contributionRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private LoanRepaymentRepository loanRepaymentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private StokvelGroupRepository groupRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private BlockchainService blockchainService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private final UUID groupId = UUID.randomUUID();
    private UserEntity adminUser;
    private UserEntity memberUser;
    private MemberEntity thabo;
    private MemberEntity lerato;
    private MemberEntity sipho;

    @BeforeEach
    void setUp() {
        adminUser = UserEntity.builder().userId(UUID.randomUUID()).fullName("Thabo Mokoena").email("thabo@example.com").build();
        memberUser = UserEntity.builder().userId(UUID.randomUUID()).fullName("Lerato Dlamini").email("lerato@example.com").build();

        StokvelGroupEntity group = StokvelGroupEntity.builder()
                .groupId(groupId)
                .groupName("Mamelodi Savings Circle")
                .adminUser(adminUser)
                .payoutCycle(PayoutCycle.MONTHLY)
                .contributionAmount(new BigDecimal("400"))
                .contributionFrequency(ContributionFrequency.MONTHLY)
                .createdAt(LocalDateTime.now().minusMonths(2))
                .inviteCode("TEST1234")
                .build();

        thabo = member("Thabo Mokoena", 1, adminUser, group);
        lerato = member("Lerato Dlamini", 2, memberUser, group);
        sipho = member("Sipho Nkosi", 3, UserEntity.builder().userId(UUID.randomUUID()).build(), group);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(currentUserService.get()).thenReturn(adminUser);
        when(memberRepository.findByGroup_GroupIdAndStatusOrderByPayoutPositionAsc(groupId, MemberStatus.ACTIVE))
                .thenReturn(List.of(thabo, lerato, sipho));
        when(memberRepository.findByUser_UserIdAndGroup_GroupId(any(), eq(groupId))).thenReturn(Optional.of(thabo));

        when(payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedFalse(groupId)).thenReturn(Optional.empty());
        when(payoutRepository.findFirstByGroup_GroupIdOrderByCreatedAtDesc(groupId)).thenReturn(Optional.empty());
        when(payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedTrueOrderByCreatedAtDesc(groupId)).thenReturn(Optional.empty());
        when(payoutRepository.findByGroup_GroupIdOrderByCreatedAtDesc(groupId)).thenReturn(List.of());

        balance("1200", "0", "0", "0");
        when(contributionRepository.existsByMember_MemberIdAndBlockchainConfirmedTrueAndContributionDateGreaterThanEqual(any(), any()))
                .thenReturn(true);
    }

    // ── Rotation ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("The first payout goes to payout position 1")
    void firstPayoutGoesToPositionOne() {
        PayoutPreviewResponse preview = payoutService.previewNextPayout(groupId);

        assertThat(preview.getRecipientPosition()).isEqualTo(1);
        assertThat(preview.getRecipientName()).isEqualTo("Thabo Mokoena");
        assertThat(preview.getRotation()).hasSize(3);
        assertThat(preview.getRotation().get(0).isNext()).isTrue();
    }

    @Test
    @DisplayName("After position 1 is paid, the next payout goes to position 2")
    void nextPayoutFollowsTheRotation() {
        lastPayoutWentTo(thabo);

        assertThat(payoutService.previewNextPayout(groupId).getRecipientPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("After the last member is paid, the rotation starts again at position 1")
    void rotationWrapsAround() {
        lastPayoutWentTo(sipho);

        assertThat(payoutService.previewNextPayout(groupId).getRecipientPosition()).isEqualTo(1);
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Available = contributions + repayments − payouts − disbursed loans")
    void availableBalanceFormula() {
        balance("1200", "300", "400", "500");

        assertThat(payoutService.previewNextPayout(groupId).getAvailableAmount()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Members without a contribution since the last payout are flagged")
    void missingContributorsAreListed() {
        when(contributionRepository.existsByMember_MemberIdAndBlockchainConfirmedTrueAndContributionDateGreaterThanEqual(eq(sipho.getMemberId()), any()))
                .thenReturn(false);

        assertThat(payoutService.previewNextPayout(groupId).getMissingContributors()).containsExactly("Sipho Nkosi");
    }

    // ── Processing ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Only the group admin can process a payout")
    void nonAdminIsForbidden() {
        when(currentUserService.get()).thenReturn(memberUser);

        assertThatThrownBy(() -> payoutService.processNextPayout(groupId))
                .isInstanceOf(AccessDeniedException.class);
        verify(blockchainService, never()).recordTransaction(any(), any(), any(), any());
    }

    @Test
    @DisplayName("A payout is refused while a previous one is unconfirmed on-chain")
    void pendingPayoutBlocksANewOne() {
        when(payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedFalse(groupId))
                .thenReturn(Optional.of(PayoutEntity.builder().payoutId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> payoutService.processNextPayout(groupId))
                .isInstanceOf(PayoutException.class)
                .hasMessageContaining("awaiting blockchain confirmation");
    }

    @Test
    @DisplayName("A payout is refused when the group has no funds")
    void noFundsNoPayout() {
        balance("0", "0", "0", "0");

        assertThatThrownBy(() -> payoutService.processNextPayout(groupId))
                .isInstanceOf(PayoutException.class)
                .hasMessageContaining("no funds");
    }

    @Test
    @DisplayName("A payout is recorded on-chain as PAYOUT and confirmed with its tx hash")
    void payoutIsRecordedOnChain() {
        String txHash = "0x" + "ab".repeat(32);
        when(payoutRepository.saveAndFlush(any(PayoutEntity.class))).thenAnswer(call -> {
            PayoutEntity saved = call.getArgument(0);
            saved.setPayoutId(UUID.randomUUID());
            return saved;
        });
        when(blockchainService.recordTransaction(any(), any(), any(), any())).thenReturn(txHash);

        PayoutResponse payout = payoutService.processNextPayout(groupId);

        assertThat(payout.getRecipientPosition()).isEqualTo(1);
        assertThat(payout.getAmount()).isEqualByComparingTo("1200");
        assertThat(payout.isBlockchainConfirmed()).isTrue();
        assertThat(payout.getTxHash()).isEqualTo(txHash);
        verify(blockchainService).recordTransaction(eq(BigInteger.ONE), eq(new BigInteger("1200")), eq(thabo.getWalletAddress()), anyString());
    }

    @Test
    @DisplayName("If the blockchain write fails, the payout is kept unconfirmed for a retry")
    void failedChainWriteLeavesPayoutUnconfirmed() {
        PayoutEntity[] saved = new PayoutEntity[1];
        when(payoutRepository.saveAndFlush(any(PayoutEntity.class))).thenAnswer(call -> {
            saved[0] = call.getArgument(0);
            saved[0].setPayoutId(UUID.randomUUID());
            return saved[0];
        });
        when(blockchainService.recordTransaction(any(), any(), any(), any())).thenThrow(new RuntimeException("RPC unavailable"));

        assertThatThrownBy(() -> payoutService.processNextPayout(groupId))
                .isInstanceOf(BlockchainOperationException.class);
        assertThat(saved[0].isBlockchainConfirmed()).isFalse();
        assertThat(saved[0].getTxHash()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MemberEntity member(String name, int position, UserEntity user, StokvelGroupEntity group) {
        return MemberEntity.builder()
                .memberId(UUID.randomUUID())
                .fullName(name)
                .payoutPosition(position)
                .status(MemberStatus.ACTIVE)
                .walletAddress("0x" + String.format("%040x", position))
                .user(user)
                .group(group)
                .build();
    }

    private void lastPayoutWentTo(MemberEntity member) {
        PayoutEntity last = PayoutEntity.builder().payoutId(UUID.randomUUID()).member(member).build();
        when(payoutRepository.findFirstByGroup_GroupIdOrderByCreatedAtDesc(groupId)).thenReturn(Optional.of(last));
    }

    private void balance(String contributions, String repayments, String payouts, String disbursed) {
        when(contributionRepository.sumConfirmedByGroupId(groupId)).thenReturn(new BigDecimal(contributions));
        when(loanRepaymentRepository.sumConfirmedByGroupId(groupId)).thenReturn(new BigDecimal(repayments));
        when(payoutRepository.sumConfirmedByGroupId(groupId)).thenReturn(new BigDecimal(payouts));
        when(loanRepository.sumDisbursedByGroupId(eq(groupId), any())).thenReturn(new BigDecimal(disbursed));
    }
}