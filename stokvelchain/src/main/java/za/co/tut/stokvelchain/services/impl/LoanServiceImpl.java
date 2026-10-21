package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.WalletUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import za.co.tut.stokvelchain.blockchain.EventTypeChainMapper;
import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.dto.request.LoanDecisionRequest;
import za.co.tut.stokvelchain.dto.request.ScoringRequest;
import za.co.tut.stokvelchain.dto.response.LoanResponse;
import za.co.tut.stokvelchain.dto.response.ScoreReason;
import za.co.tut.stokvelchain.dto.response.ScoringResponse;
import za.co.tut.stokvelchain.entity.*;
import za.co.tut.stokvelchain.enums.*;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.exception.LoanDecisionException;
import za.co.tut.stokvelchain.exception.LoanEligibilityException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.repository.*;
import za.co.tut.stokvelchain.scoring.BehavioralFeatureCalculator;
import za.co.tut.stokvelchain.scoring.CreditScoringClient;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.LoanService;
import za.co.tut.stokvelchain.services.NotificationService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {
    private static final int MIN_CONFIRMED_CONTRIBUTIONS = 3;
    private static final int MIN_OVERRIDE_REASON_LENGTH = 20;
    private static final int MAX_REASONS_IN_NOTIFICATION = 3;
    private static final List<LoanStatus> OPEN_STATUSES =
            List.of(LoanStatus.PENDING, LoanStatus.APPROVED_PENDING_DISBURSEMENT);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LoanRepository loanRepository;
    private final BehavioralScoreRepository behavioralScoreRepository;
    private final MemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final TransactionRepository transactionRepository;
    private final StokvelGroupRepository groupRepository;
    private final CurrentUserService currentUserService;
    private final BehavioralFeatureCalculator featureCalculator;
    private final CreditScoringClient creditScoringClient;
    private final BlockchainService blockchainService;
    private final NotificationService notificationService;
    private final InterestRatePolicy interestRatePolicy;
    private final LoanRepaymentRepository loanRepaymentRepository;

    // ── Member: apply ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LoanResponse applyForLoan(UUID groupId, LoanApplicationRequest request) {
        MemberEntity member = getCurrentMember(groupId);
        checkEligibility(member);

        // 1. Score first: if the scoring service is unavailable, nothing is saved.
        ScoringRequest features = featureCalculator.calculate(member, request.getAmount());
        ScoringResponse scoring = creditScoringClient.score(features);

        // 2. Persist the application with the model's score.
        LoanEntity loan = LoanEntity.builder()
                .member(member)
                .amount(request.getAmount())
                .repaymentMonths(request.getRepaymentMonths())
                .aiScore(toDecimal(scoring.getProbability()))
                .build();
        loan = loanRepository.saveAndFlush(loan);

        // 3. Snapshot exactly what the model saw, for audit and future retraining.
        BehavioralScoreEntity score = BehavioralScoreEntity.builder()
                .member(member)
                .loan(loan)
                .contributionFreq(features.getContributionFrequencyScore())
                .payoutCompliance(features.getPayoutComplianceRate())
                .repaymentRatio(features.getLoanRepaymentRatio())
                .creditAmountNormalised(features.getCreditAmountNormalised())
                .age(features.getAge())
                .tenureMonths(tenureMonths(member))
                .aiScore(loan.getAiScore())
                .threshold(toDecimal(scoring.getThreshold()))
                .recommendation(scoring.isEligible() ? ModelRecommendation.APPROVE : ModelRecommendation.DECLINE)
                .modelVersion(scoring.getModelVersion())
                .reasonsJson(JSON.writeValueAsString(scoring.getReasons()))
                .build();
        score = behavioralScoreRepository.save(score);

        // 4. Explainability: tell the group admin why the model recommends declining.
        if (score.getRecommendation() == ModelRecommendation.DECLINE) {
            notifyAdminOfDecline(member, loan, scoring);
        }

        return toResponse(loan, score);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getMyLoans(UUID groupId) {
        MemberEntity member = getCurrentMember(groupId);
        return loanRepository.findByMember_MemberIdOrderByRequestedAtDesc(member.getMemberId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Admin: review and decide ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getGroupLoans(UUID groupId, LoanStatus status) {
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, currentUserService.get());

        List<LoanEntity> loans = status == null
                ? loanRepository.findByMember_GroupOrderByRequestedAtDesc(group)
                : loanRepository.findByMember_GroupAndStatusOrderByRequestedAtDesc(group, status);
        return loans.stream().map(this::toResponse).toList();
    }

    /**
     * Admin approves or rejects a pending application. When the decision differs from
     * the credit model's recommendation, a category and written reason are mandatory —
     * they are kept as labelled data for retraining future models.
     *
     * noRollbackFor: if the on-chain disbursement fails, the approval itself is kept
     * (status APPROVED_PENDING_DISBURSEMENT) so the admin can retry the disbursement.
     */
    @Override
    @Transactional(noRollbackFor = BlockchainOperationException.class)
    public LoanResponse decideLoan(UUID groupId, UUID loanId, LoanDecisionRequest request) {
        UserEntity admin = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, admin);

        LoanEntity loan = findLoanInGroup(group, loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanDecisionException("Only pending loan applications can be decided");
        }

        BehavioralScoreEntity score = behavioralScoreRepository.findByLoan_LoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit score not found for loan " + loanId));

        boolean approve = request.getApprove();
        boolean modelApproved = score.getRecommendation() == ModelRecommendation.APPROVE;
        boolean overridden = approve != modelApproved;
        if (overridden) {
            validateOverride(request);
            loan.setOverrideCategory(request.getOverrideCategory());
            loan.setOverrideReason(request.getOverrideReason().trim());
        }

        loan.setOverridden(overridden);
        loan.setDecidedBy(admin);
        loan.setResolvedAt(LocalDateTime.now());

        if (!approve) {
            loan.setStatus(LoanStatus.REJECTED);
            loan = loanRepository.save(loan);
            notificationService.notify(loan.getMember().getUser(), group, NotificationType.LOAN_REJECTED,
                    "Loan application declined",
                    String.format(Locale.ROOT, "Your loan application of R%s was declined by the group admin.",
                            loan.getAmount().toPlainString()),
                    loan.getLoanId());
            return toResponse(loan, score);
        }

        // Approved: persist the approval first, then disburse on-chain.
        loan.setStatus(LoanStatus.APPROVED_PENDING_DISBURSEMENT);
        BigDecimal rate = interestRatePolicy.rateFor(loan.getAiScore(), score.getThreshold());
        loan.setInterestRate(rate);
        loan.setTotalRepayable(loan.getAmount().multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP));
        loan = loanRepository.saveAndFlush(loan);

        disburse(loan);
        notifyMemberOfApproval(loan, group);
        return toResponse(loan, score);
    }

    @Override
    @Transactional(noRollbackFor = BlockchainOperationException.class)
    public LoanResponse retryDisbursement(UUID groupId, UUID loanId) {
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, currentUserService.get());

        LoanEntity loan = findLoanInGroup(group, loanId);
        if (loan.getStatus() != LoanStatus.APPROVED_PENDING_DISBURSEMENT) {
            throw new LoanDecisionException("Only approved loans awaiting disbursement can be retried");
        }

        disburse(loan);
        notifyMemberOfApproval(loan, group);
        return toResponse(loan);
    }

    // ── Disbursement ──────────────────────────────────────────────────────────

    private void disburse(LoanEntity loan) {
        MemberEntity member = loan.getMember();

        BigInteger chainEventType = EventTypeChainMapper.toChainOrdinal(EventType.LOAN_DISBURSEMENT);
        String txHash;
        try {
            txHash = blockchainService.recordTransaction(
                    chainEventType,
                    loan.getAmount().toBigInteger(),
                    member.getWalletAddress(),
                    loan.getLoanId().toString()
            );
        } catch (Exception ex) {
            // Loan stays APPROVED_PENDING_DISBURSEMENT (no rollback) — retryable.
            throw new BlockchainOperationException(
                    "Loan approved but on-chain disbursement failed for id " + loan.getLoanId()
                            + "; retry the disbursement", ex);
        }

        loan.setTxHash(txHash);
        loan.setStatus(LoanStatus.APPROVED);
        loanRepository.save(loan);

        TransactionEntity txn = new TransactionEntity();
        txn.setMember(member);
        txn.setSourceTable("loans");
        txn.setSourceRecordId(loan.getLoanId());
        txn.setAmount(loan.getAmount());
        txn.setTxHash(txHash);
        txn.setEventType(EventType.LOAN_DISBURSEMENT);
        transactionRepository.save(txn);
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    private void checkEligibility(MemberEntity member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new LoanEligibilityException("Only active members can apply for a loan");
        }
        if (member.isLoanRestricted()) {
            throw new LoanEligibilityException(
                    "Loan applications are restricted until outstanding contributions are settled");
        }
        if (member.getWalletAddress() == null || !WalletUtils.isValidAddress(member.getWalletAddress())) {
            throw new LoanEligibilityException("Member has no valid wallet address; cannot disburse a loan");
        }
        long confirmed = contributionRepository.countByMember_MemberIdAndBlockchainConfirmedTrue(member.getMemberId());
        if (confirmed < MIN_CONFIRMED_CONTRIBUTIONS) {
            throw new LoanEligibilityException("At least " + MIN_CONFIRMED_CONTRIBUTIONS
                    + " confirmed contributions are required before requesting a loan (currently " + confirmed + ")");
        }
        if (loanRepository.existsByMember_MemberIdAndStatusIn(member.getMemberId(), OPEN_STATUSES)) {
            throw new LoanEligibilityException("You already have a loan application in progress");
        }
    }

    private void validateOverride(LoanDecisionRequest request) {
        if (request.getOverrideCategory() == null) {
            throw new LoanDecisionException(
                    "An override category is required when the decision differs from the credit model's recommendation");
        }
        String reason = request.getOverrideReason();
        if (reason == null || reason.trim().length() < MIN_OVERRIDE_REASON_LENGTH) {
            throw new LoanDecisionException("A written reason of at least " + MIN_OVERRIDE_REASON_LENGTH
                    + " characters is required when overriding the credit model");
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void notifyAdminOfDecline(MemberEntity member, LoanEntity loan, ScoringResponse scoring) {
        String reasons = scoring.getReasons() == null ? "" : scoring.getReasons().stream()
                .filter(r -> r.getImpact() < 0)
                .limit(MAX_REASONS_IN_NOTIFICATION)
                .map(ScoreReason::getMessage)
                .collect(Collectors.joining("; "));

        String message = String.format(Locale.ROOT,
                "%s requested a loan of R%s over %d months. The credit model recommends declining: "
                        + "score %.1f/100 (minimum %.1f). Main reasons: %s. "
                        + "You may still approve it by giving a documented reason.",
                member.getFullName(),
                loan.getAmount().toPlainString(),
                loan.getRepaymentMonths(),
                scoring.getScore(),
                scoring.getThreshold() * 100,
                reasons.isEmpty() ? "no single dominant factor" : reasons);

        notificationService.notify(member.getGroup().getAdminUser(), member.getGroup(),
                NotificationType.LOAN_DECLINED_BY_MODEL, "Loan application flagged by credit model",
                message, loan.getLoanId());
    }

    private void notifyMemberOfApproval(LoanEntity loan, StokvelGroupEntity group) {
        notificationService.notify(loan.getMember().getUser(), group, NotificationType.LOAN_APPROVED,
                "Loan approved",
                String.format(Locale.ROOT,
                        "Your loan of R%s has been approved and disbursed. Interest: %s%% (based on your credit score). Total to repay: R%s over %d months.",
                        loan.getAmount().toPlainString(),
                        loan.getInterestRate() != null ? loan.getInterestRate().movePointRight(2).stripTrailingZeros().toPlainString() : "0",
                        (loan.getTotalRepayable() != null ? loan.getTotalRepayable() : loan.getAmount()).toPlainString(),
                        loan.getRepaymentMonths()),
                loan.getLoanId());

    }

    // ── Lookups and helpers ───────────────────────────────────────────────────

    private MemberEntity getCurrentMember(UUID groupId) {
        UserEntity user = currentUserService.get();
        return memberRepository.findByUser_EmailAndGroup_GroupId(user.getEmail(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));
    }

    private StokvelGroupEntity findGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
    }

    private void requireAdmin(StokvelGroupEntity group, UserEntity user) {
        if (!group.getAdminUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Only the group admin can perform this action");
        }
    }

    private LoanEntity findLoanInGroup(StokvelGroupEntity group, UUID loanId) {
        return loanRepository.findById(loanId)
                .filter(loan -> loan.getMember().getGroup().getGroupId().equals(group.getGroupId()))
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found in this group: " + loanId));
    }

    private int tenureMonths(MemberEntity member) {
        LocalDateTime start = member.getActivatedAt() != null ? member.getActivatedAt() : member.getJoinedAt();
        return (int) ChronoUnit.MONTHS.between(start, LocalDateTime.now());
    }

    private BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private LoanResponse toResponse(LoanEntity loan) {
        return toResponse(loan, behavioralScoreRepository.findByLoan_LoanId(loan.getLoanId()).orElse(null));
    }

    private LoanResponse toResponse(LoanEntity loan, BehavioralScoreEntity score) {
        BigDecimal totalRepayable = loan.getTotalRepayable() != null ? loan.getTotalRepayable() : loan.getAmount();
        BigDecimal totalRepaid = loanRepaymentRepository.sumAmountByLoanId(loan.getLoanId());
        boolean repayable = loan.getStatus() == LoanStatus.APPROVED || loan.getStatus() == LoanStatus.REPAID;
        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .memberName(loan.getMember().getFullName())
                .amount(loan.getAmount())
                .repaymentMonths(loan.getRepaymentMonths())
                .status(loan.getStatus())
                .aiScore(loan.getAiScore())
                .threshold(score != null ? score.getThreshold() : null)
                .recommendation(score != null ? score.getRecommendation() : null)
                .reasons(score != null ? readReasons(score.getReasonsJson()) : List.of())
                .overridden(loan.isOverridden())
                .overrideCategory(loan.getOverrideCategory())
                .overrideReason(loan.getOverrideReason())
                .requestedAt(loan.getRequestedAt())
                .resolvedAt(loan.getResolvedAt())
                .txHash(loan.getTxHash())
                .interestRate(loan.getInterestRate())
                .totalRepayable(totalRepayable)
                .totalRepaid(totalRepaid)
                .outstanding(repayable ? totalRepayable.subtract(totalRepaid).max(BigDecimal.ZERO) : null)
                .instalmentAmount(totalRepayable.divide(BigDecimal.valueOf(loan.getRepaymentMonths()), 2, RoundingMode.HALF_UP))
                .build();
    }

    private List<ScoreReason> readReasons(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<ScoreReason> reasons = JSON.readValue(json, new TypeReference<List<ScoreReason>>() {});
        return reasons != null ? reasons : List.of();
    }
}
