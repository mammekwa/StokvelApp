package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.WalletUtils;
import za.co.tut.stokvelchain.blockchain.EventTypeChainMapper;
import za.co.tut.stokvelchain.dto.response.PayoutPreviewResponse;
import za.co.tut.stokvelchain.dto.response.PayoutResponse;
import za.co.tut.stokvelchain.dto.response.PayoutRotationEntry;
import za.co.tut.stokvelchain.entity.*;
import za.co.tut.stokvelchain.enums.*;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.exception.PayoutException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.repository.*;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.NotificationService;
import za.co.tut.stokvelchain.services.PayoutService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {
    private static final List<LoanStatus> DISBURSED_STATUSES =
            List.of(LoanStatus.APPROVED, LoanStatus.REPAID, LoanStatus.DEFAULTED);

    private final PayoutRepository payoutRepository;
    private final MemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final TransactionRepository transactionRepository;
    private final StokvelGroupRepository groupRepository;
    private final CurrentUserService currentUserService;
    private final BlockchainService blockchainService;
    private final NotificationService notificationService;

    // ── Preview (any group member) ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PayoutPreviewResponse previewNextPayout(UUID groupId) {
        StokvelGroupEntity group = findGroup(groupId);
        requireMember(groupId, currentUserService.get());

        List<MemberEntity> rotation = activeRotation(groupId);
        List<PayoutEntity> payouts = payoutRepository.findByGroup_GroupIdOrderByCreatedAtDesc(groupId);
        MemberEntity next = rotation.isEmpty() ? null : nextRecipient(groupId, rotation);

        Optional<PayoutEntity> lastConfirmed =
                payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedTrueOrderByCreatedAtDesc(groupId);
        LocalDate dueDate = nextDueDate(group, lastConfirmed);

        return PayoutPreviewResponse.builder()
                .recipientMemberId(next != null ? next.getMemberId() : null)
                .recipientName(next != null ? next.getFullName() : null)
                .recipientPosition(next != null ? next.getPayoutPosition() : null)
                .availableAmount(availableBalance(groupId))
                .dueDate(dueDate)
                .early(LocalDate.now().isBefore(dueDate))
                .missingContributors(missingContributors(group, rotation, lastConfirmed))
                .pendingPayoutId(payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedFalse(groupId)
                        .map(PayoutEntity::getPayoutId).orElse(null))
                .rotation(rotationEntries(rotation, next, payouts))
                .build();
    }

    // ── Process (admin) ───────────────────────────────────────────────────────

    /**
     * Pays the group balance to the next member in the rotation.
     * Persist -> write on-chain (PAYOUT) -> confirm, same as contributions and loans.
     *
     * noRollbackFor: if the chain write fails, the payout stays unconfirmed and
     * must be retried before any further payout can be made.
     */
    @Override
    @Transactional(noRollbackFor = BlockchainOperationException.class)
    public PayoutResponse processNextPayout(UUID groupId) {
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, currentUserService.get());

        payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedFalse(groupId).ifPresent(p -> {
            throw new PayoutException("A previous payout is still awaiting blockchain confirmation; retry it first");
        });

        List<MemberEntity> rotation = activeRotation(groupId);
        if (rotation.isEmpty()) {
            throw new PayoutException("There are no active members in the payout rotation");
        }
        MemberEntity recipient = nextRecipient(groupId, rotation);
        if (recipient.getWalletAddress() == null || !WalletUtils.isValidAddress(recipient.getWalletAddress())) {
            throw new PayoutException(recipient.getFullName() + " has no valid wallet address; cannot record the payout");
        }

        BigDecimal amount = availableBalance(groupId);
        if (amount.signum() <= 0) {
            throw new PayoutException("There are no funds available to pay out");
        }

        Optional<PayoutEntity> lastConfirmed =
                payoutRepository.findFirstByGroup_GroupIdAndBlockchainConfirmedTrueOrderByCreatedAtDesc(groupId);

        // 1. Persist first (unconfirmed) so the payout has an id for the chain.
        PayoutEntity payout = PayoutEntity.builder()
                .group(group)
                .member(recipient)
                .amount(amount)
                .scheduledDate(nextDueDate(group, lastConfirmed))
                .build();
        payout = payoutRepository.saveAndFlush(payout);

        // 2. Write on-chain and confirm.
        recordOnChain(payout);
        return toResponse(payout);
    }

    @Override
    @Transactional(noRollbackFor = BlockchainOperationException.class)
    public PayoutResponse retryPayout(UUID groupId, UUID payoutId) {
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, currentUserService.get());

        PayoutEntity payout = payoutRepository.findById(payoutId)
                .filter(p -> p.getGroup().getGroupId().equals(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found in this group: " + payoutId));
        if (payout.isBlockchainConfirmed()) {
            throw new PayoutException("This payout is already confirmed on the blockchain");
        }

        recordOnChain(payout);
        return toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> getGroupPayouts(UUID groupId) {
        findGroup(groupId);
        requireMember(groupId, currentUserService.get());
        return payoutRepository.findByGroup_GroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Blockchain ────────────────────────────────────────────────────────────

    private void recordOnChain(PayoutEntity payout) {
        MemberEntity recipient = payout.getMember();

        BigInteger chainEventType = EventTypeChainMapper.toChainOrdinal(EventType.PAYOUT);
        String txHash;
        try {
            txHash = blockchainService.recordTransaction(
                    chainEventType,
                    payout.getAmount().toBigInteger(),
                    recipient.getWalletAddress(),
                    payout.getPayoutId().toString()
            );
        } catch (Exception ex) {
            throw new BlockchainOperationException(
                    "Payout saved but on-chain recording failed for id " + payout.getPayoutId() + "; retry the payout", ex);
        }

        payout.setTxHash(txHash);
        payout.setBlockchainConfirmed(true);
        payout.setProcessedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        TransactionEntity txn = new TransactionEntity();
        txn.setMember(recipient);
        txn.setSourceTable("payouts");
        txn.setSourceRecordId(payout.getPayoutId());
        txn.setAmount(payout.getAmount());
        txn.setTxHash(txHash);
        txn.setEventType(EventType.PAYOUT);
        transactionRepository.save(txn);

        try {
            notificationService.notify(recipient.getUser(), payout.getGroup(), NotificationType.PAYOUT_RECEIVED,
                    "Payout received",
                    String.format(Locale.ROOT, "You received this cycle's payout of R%s from %s.",
                            payout.getAmount().toPlainString(), payout.getGroup().getGroupName()),
                    payout.getPayoutId());
        } catch (Exception ex) {
            log.warn("Payout recorded but notification failed for payout {}", payout.getPayoutId(), ex);
        }
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    private List<MemberEntity> activeRotation(UUID groupId) {
        return memberRepository.findByGroup_GroupIdAndStatusOrderByPayoutPositionAsc(groupId, MemberStatus.ACTIVE)
                .stream()
                .filter(m -> m.getPayoutPosition() != null)
                .toList();
    }

    /** The active member with the next payout position after the last payout; wraps to the start for a new round. */
    private MemberEntity nextRecipient(UUID groupId, List<MemberEntity> rotation) {
        int lastPosition = payoutRepository.findFirstByGroup_GroupIdOrderByCreatedAtDesc(groupId)
                .map(p -> p.getMember().getPayoutPosition())
                .orElse(0);
        return rotation.stream()
                .filter(m -> m.getPayoutPosition() > lastPosition)
                .findFirst()
                .orElse(rotation.get(0));
    }

    /** Money held by the group: contributions + repayments − payouts − disbursed loans (confirmed only). */
    private BigDecimal availableBalance(UUID groupId) {
        BigDecimal contributions = contributionRepository.sumConfirmedByGroupId(groupId);
        BigDecimal repayments = loanRepaymentRepository.sumConfirmedByGroupId(groupId);
        BigDecimal payouts = payoutRepository.sumConfirmedByGroupId(groupId);
        BigDecimal disbursed = loanRepository.sumDisbursedByGroupId(groupId, DISBURSED_STATUSES);
        return contributions.add(repayments).subtract(payouts).subtract(disbursed).max(BigDecimal.ZERO);
    }

    private LocalDate nextDueDate(StokvelGroupEntity group, Optional<PayoutEntity> lastConfirmed) {
        LocalDate base = lastConfirmed.map(PayoutEntity::getScheduledDate)
                .orElse(group.getCreatedAt().toLocalDate());
        return base.plusMonths(cycleMonths(group.getPayoutCycle()));
    }

    private int cycleMonths(PayoutCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case BIANNUALLY -> 6;
            case ANNUALLY -> 12;
        };
    }

    /** Active members with no confirmed contribution since the last payout (or since the group started). */
    private List<String> missingContributors(StokvelGroupEntity group, List<MemberEntity> rotation,
                                             Optional<PayoutEntity> lastConfirmed) {
        LocalDate since = lastConfirmed
                .map(p -> p.getProcessedAt() != null ? p.getProcessedAt().toLocalDate() : p.getScheduledDate())
                .orElse(group.getCreatedAt().toLocalDate());
        return rotation.stream()
                .filter(m -> !contributionRepository
                        .existsByMember_MemberIdAndBlockchainConfirmedTrueAndContributionDateGreaterThanEqual(
                                m.getMemberId(), since))
                .map(MemberEntity::getFullName)
                .toList();
    }

    private List<PayoutRotationEntry> rotationEntries(List<MemberEntity> rotation, MemberEntity next,
                                                      List<PayoutEntity> payouts) {
        Map<UUID, LocalDateTime> lastPaid = new HashMap<>();
        for (PayoutEntity p : payouts) {
            if (p.isBlockchainConfirmed() && p.getProcessedAt() != null) {
                lastPaid.merge(p.getMember().getMemberId(), p.getProcessedAt(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return rotation.stream()
                .map(m -> PayoutRotationEntry.builder()
                        .memberId(m.getMemberId())
                        .memberName(m.getFullName())
                        .payoutPosition(m.getPayoutPosition())
                        .next(next != null && m.getMemberId().equals(next.getMemberId()))
                        .lastPaidAt(lastPaid.get(m.getMemberId()))
                        .build())
                .toList();
    }

    // ── Lookups and helpers ───────────────────────────────────────────────────

    private StokvelGroupEntity findGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private void requireAdmin(StokvelGroupEntity group, UserEntity user) {
        if (!group.getAdminUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Only the group admin can process payouts");
        }
    }

    private void requireMember(UUID groupId, UserEntity user) {
        memberRepository.findByUser_UserIdAndGroup_GroupId(user.getUserId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));
    }

    private PayoutResponse toResponse(PayoutEntity payout) {
        return PayoutResponse.builder()
                .payoutId(payout.getPayoutId())
                .recipientMemberId(payout.getMember().getMemberId())
                .recipientName(payout.getMember().getFullName())
                .recipientPosition(payout.getMember().getPayoutPosition())
                .amount(payout.getAmount())
                .scheduledDate(payout.getScheduledDate())
                .processedAt(payout.getProcessedAt())
                .txHash(payout.getTxHash())
                .blockchainConfirmed(payout.isBlockchainConfirmed())
                .build();
    }
}
