package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.WalletUtils;
import za.co.tut.stokvelchain.blockchain.EventTypeChainMapper;
import za.co.tut.stokvelchain.dto.request.LoanRepaymentRequest;
import za.co.tut.stokvelchain.dto.response.LoanRepaymentResponse;
import za.co.tut.stokvelchain.entity.*;
import za.co.tut.stokvelchain.enums.EventType;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.enums.NotificationType;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.exception.RepaymentException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.repository.LoanRepaymentRepository;
import za.co.tut.stokvelchain.repository.LoanRepository;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.repository.TransactionRepository;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.LoanRepaymentService;
import za.co.tut.stokvelchain.services.NotificationService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanRepaymentServiceImpl implements LoanRepaymentService {
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final BlockchainService blockchainService;
    private final NotificationService notificationService;

    /**
     * Member repays part or all of their own disbursed loan.
     * Persist -> write on-chain (LOAN_REPAYMENT) -> confirm, same as contributions.
     *
     * noRollbackFor: if the chain write fails, the repayment row is kept as
     * unconfirmed. Only confirmed repayments reduce the outstanding balance.
     */
    @Override
    @Transactional(noRollbackFor = BlockchainOperationException.class)
    public LoanRepaymentResponse repay(UUID groupId, UUID loanId, LoanRepaymentRequest request) {
        MemberEntity member = getCurrentMember(groupId);
        LoanEntity loan = loanRepository.findById(loanId)
                .filter(l -> l.getMember().getMemberId().equals(member.getMemberId()))
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new RepaymentException("Only disbursed loans can be repaid (this loan is " + loan.getStatus() + ")");
        }
        if (member.getWalletAddress() == null || !WalletUtils.isValidAddress(member.getWalletAddress())) {
            throw new RepaymentException("Member has no valid wallet address; cannot record repayment on-chain");
        }

        BigDecimal outstanding = outstanding(loan);
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new RepaymentException(String.format(Locale.ROOT,
                    "Repayment of R%s exceeds the outstanding balance of R%s",
                    request.getAmount().toPlainString(), outstanding.toPlainString()));
        }

        // 1. Persist first (unconfirmed) so the record has an id for the chain.
        LoanRepaymentEntity repayment = LoanRepaymentEntity.builder()
                .loan(loan)
                .amount(request.getAmount())
                .paymentDate(LocalDate.now())
                .paymentReference(request.getPaymentReference())
                .build();
        repayment = loanRepaymentRepository.saveAndFlush(repayment);

        // 2. Write to chain.
        BigInteger chainEventType = EventTypeChainMapper.toChainOrdinal(EventType.LOAN_REPAYMENT);
        String txHash;
        try {
            txHash = blockchainService.recordTransaction(
                    chainEventType,
                    request.getAmount().toBigInteger(),
                    member.getWalletAddress(),
                    repayment.getRepaymentId().toString()
            );
        } catch (Exception ex) {
            throw new BlockchainOperationException(
                    "Repayment saved but on-chain recording failed for id " + repayment.getRepaymentId(), ex);
        }

        // 3. Confirm.
        repayment.setTxHash(txHash);
        repayment.setBlockchainConfirmed(true);
        repayment = loanRepaymentRepository.save(repayment);

        TransactionEntity txn = new TransactionEntity();
        txn.setMember(member);
        txn.setSourceTable("loan_repayments");
        txn.setSourceRecordId(repayment.getRepaymentId());
        txn.setAmount(request.getAmount());
        txn.setTxHash(txHash);
        txn.setEventType(EventType.LOAN_REPAYMENT);
        transactionRepository.save(txn);

        // 4. Settle the loan when nothing is left.
        BigDecimal remaining = outstanding.subtract(request.getAmount());
        if (remaining.signum() <= 0) {
            loan.setStatus(LoanStatus.REPAID);
            loanRepository.save(loan);
            notifyLoanRepaid(loan, member);
        }

        return toResponse(repayment, remaining.max(BigDecimal.ZERO));
    }

    /** The loan's own member, or the group admin, can view its repayments. */
    @Override
    @Transactional(readOnly = true)
    public List<LoanRepaymentResponse> getRepayments(UUID groupId, UUID loanId) {
        UserEntity user = currentUserService.get();
        LoanEntity loan = loanRepository.findById(loanId)
                .filter(l -> l.getMember().getGroup().getGroupId().equals(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found in this group: " + loanId));

        boolean isOwner = loan.getMember().getUser().getUserId().equals(user.getUserId());
        boolean isAdmin = loan.getMember().getGroup().getAdminUser().getUserId().equals(user.getUserId());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only view repayments for your own loans");
        }

        return loanRepaymentRepository.findByLoan_LoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(r -> toResponse(r, null))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Amount owed including interest; loans approved before interest existed owe the principal only. */
    private BigDecimal outstanding(LoanEntity loan) {
        BigDecimal totalRepayable = loan.getTotalRepayable() != null ? loan.getTotalRepayable() : loan.getAmount();
        BigDecimal repaid = loanRepaymentRepository.sumAmountByLoanId(loan.getLoanId());
        return totalRepayable.subtract(repaid).max(BigDecimal.ZERO);
    }

    private void notifyLoanRepaid(LoanEntity loan, MemberEntity member) {
        StokvelGroupEntity group = member.getGroup();

        try {
            notificationService.notify(member.getUser(), group, NotificationType.LOAN_REPAID,
                    "Loan fully repaid",
                    String.format(Locale.ROOT, "Your loan of R%s is fully repaid. Thank you!",
                            loan.getAmount().toPlainString()),
                    loan.getLoanId());
            notificationService.notify(group.getAdminUser(), group, NotificationType.LOAN_REPAID,
                    "Loan fully repaid",
                    String.format(Locale.ROOT, "%s has fully repaid their loan of R%s.",
                            member.getFullName(), loan.getAmount().toPlainString()),
                    loan.getLoanId());
        } catch (Exception ex) {
            log.warn("Repayment saved but notification failed for loan {}", loan.getLoanId(), ex);
        }
    }

    private MemberEntity getCurrentMember(UUID groupId) {
        UserEntity user = currentUserService.get();
        return memberRepository.findByUser_UserIdAndGroup_GroupId(user.getUserId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));
    }

    private LoanRepaymentResponse toResponse(LoanRepaymentEntity repayment, BigDecimal remainingBalance) {
        return LoanRepaymentResponse.builder()
                .repaymentId(repayment.getRepaymentId())
                .loanId(repayment.getLoan().getLoanId())
                .amount(repayment.getAmount())
                .paymentDate(repayment.getPaymentDate())
                .paymentReference(repayment.getPaymentReference())
                .txHash(repayment.getTxHash())
                .blockchainConfirmed(repayment.isBlockchainConfirmed())
                .remainingBalance(remainingBalance)
                .build();
    }
}
