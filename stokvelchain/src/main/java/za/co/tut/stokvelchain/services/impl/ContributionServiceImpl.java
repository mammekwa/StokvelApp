package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tut.stokvelchain.blockchain.EventTypeChainMapper;
import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.*;
import za.co.tut.stokvelchain.entity.*;
import za.co.tut.stokvelchain.enums.ContributionFrequency;
import za.co.tut.stokvelchain.enums.EventType;
import za.co.tut.stokvelchain.enums.MemberStatus;
import za.co.tut.stokvelchain.exception.InvalidReferenceException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.mapper.ContributionMapper;
import za.co.tut.stokvelchain.repository.ContributionRepository;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.repository.StokvelGroupRepository;
import za.co.tut.stokvelchain.repository.TransactionRepository;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.ContributionService;
import za.co.tut.stokvelchain.services.CurrentUserService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionServiceImpl implements ContributionService {
    private final ContributionRepository contributionRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final BlockchainService blockchainService;
    private final ContributionMapper contributionMapper;
    private final CurrentUserService currentUserService;
    private final StokvelGroupRepository groupRepository;

    @Override
    @Transactional
    public ContributionResponse logContribution(UUID groupId, ContributionRequest request) {
        MemberEntity member = getCurrentMember(groupId);

        // 1. Persist first (unconfirmed) so we have a contributionId to use
        //    as the on-chain sourceRecordId even if the chain call fails.
        ContributionEntity contribution = new ContributionEntity();
        contribution.setMember(member);
        contribution.setAmount(request.getAmount());
        contribution.setContributionDate(LocalDate.now());
        contribution.setPaymentReference(request.getPaymentReference());
        contribution.setBlockchainConfirmed(false);
        contribution = contributionRepository.save(contribution);

        // 2. Write to chain via the explicit EventType mapping.
        BigInteger chainEventType = EventTypeChainMapper.toChainOrdinal(EventType.CONTRIBUTION);
        BigInteger chainAmount = request.getAmount().toBigInteger(); // see note below on precision
        String txHash;
        try {
            txHash = blockchainService.recordTransaction(
                    chainEventType,
                    chainAmount,
                    member.getWalletAddress(),
                    contribution.getContributionId().toString()
            );
        } catch (Exception ex) {
            // Contribution row stays persisted with blockchainConfirmed=false
            // and txHash=null — recoverable/retryable state, not a rollback.
            log.error("Blockchain write failed -> sourceTable=contributions, sourceRecordId={}, cause={}",
                    contribution.getContributionId(), ex.getMessage(), ex);
            return contributionMapper.toResponse(contribution);
        }

        // 3. Confirm.
        contribution.setTxHash(txHash);
        contribution.setBlockchainConfirmed(true);
        contribution = contributionRepository.save(contribution);
        contributionRepository.flush();

        TransactionEntity txn = new TransactionEntity();
        txn.setMember(member);
        txn.setSourceTable("contributions");
        txn.setSourceRecordId(contribution.getContributionId());
        txn.setAmount(request.getAmount());
        txn.setTxHash(txHash);
        txn.setEventType(EventType.CONTRIBUTION);
        transactionRepository.save(txn);

        return contributionMapper.toResponse(contribution);
    }

    public List<ContributionResponse> getMyContributions(UUID groupId) {
        MemberEntity member = getCurrentMember(groupId);
        return contributionRepository.findByMember_MemberId(member.getMemberId())
                .stream()
                .map(contributionMapper::toResponse)
                .toList();
    }

    /**
     * Admin report: every contribution in the period, plus each active member's arrears.
     * Expected = contribution amount × contributions due (counted from when the member became active).
     * Only contributions confirmed on the blockchain count as paid.
     */
    @Override
    @Transactional(readOnly = true)
    public ContributionLedgerResponse getGroupLedger(UUID groupId, LocalDate from, LocalDate to) {
        StokvelGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        if (!group.getAdminUser().getUserId().equals(currentUserService.get().getUserId())) {
            throw new AccessDeniedException("Only the group admin can view the group contribution ledger");
        }

        LocalDate start = from != null ? from : group.getCreatedAt().toLocalDate();
        LocalDate end = to != null ? to : LocalDate.now();
        if (start.isAfter(end)) {
            throw new InvalidReferenceException("'from' must be on or before 'to'");
        }

        List<ContributionEntity> contributions = contributionRepository
                .findByMember_Group_GroupIdAndContributionDateBetweenOrderByContributionDateAscCreatedAtAsc(groupId, start, end);

        List<ContributionLedgerEntry> entries = contributions.stream()
                .map(c -> ContributionLedgerEntry.builder()
                        .contributionId(c.getContributionId())
                        .memberId(c.getMember().getMemberId())
                        .memberName(c.getMember().getFullName())
                        .amount(c.getAmount())
                        .contributionDate(c.getContributionDate())
                        .paymentReference(c.getPaymentReference())
                        .txHash(c.getTxHash())
                        .blockchainConfirmed(c.isBlockchainConfirmed())
                        .build())
                .toList();

        List<MemberArrearsEntry> members = memberRepository
                .findByGroupAndStatusOrderByPayoutPositionAsc(group, MemberStatus.ACTIVE)
                .stream()
                .map(m -> arrearsFor(group, m, contributions, start, end))
                .toList();

        return ContributionLedgerResponse.builder()
                .from(start)
                .to(end)
                .contributionAmount(group.getContributionAmount())
                .contributionFrequency(group.getContributionFrequency())
                .totalExpected(members.stream().map(MemberArrearsEntry::getExpected).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalPaid(members.stream().map(MemberArrearsEntry::getPaid).reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalArrears(members.stream().map(MemberArrearsEntry::getArrears).reduce(BigDecimal.ZERO, BigDecimal::add))
                .members(members)
                .contributions(entries)
                .build();
    }

    private MemberArrearsEntry arrearsFor(StokvelGroupEntity group, MemberEntity member,
                                          List<ContributionEntity> contributions, LocalDate from, LocalDate to) {
        List<ContributionEntity> own = contributions.stream()
                .filter(c -> c.getMember().getMemberId().equals(member.getMemberId()))
                .toList();
        BigDecimal paid = own.stream().filter(ContributionEntity::isBlockchainConfirmed)
                .map(ContributionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unconfirmed = own.stream().filter(c -> !c.isBlockchainConfirmed())
                .map(ContributionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime activeSince = member.getActivatedAt() != null ? member.getActivatedAt() : member.getJoinedAt();
        LocalDate dueFrom = activeSince != null && activeSince.toLocalDate().isAfter(from) ? activeSince.toLocalDate() : from;
        int due = contributionsDue(group.getContributionFrequency(), dueFrom, to);
        BigDecimal expected = group.getContributionAmount().multiply(BigDecimal.valueOf(due));

        return MemberArrearsEntry.builder()
                .memberId(member.getMemberId())
                .memberName(member.getFullName())
                .payoutPosition(member.getPayoutPosition())
                .contributionsDue(due)
                .expected(expected)
                .paid(paid)
                .unconfirmed(unconfirmed)
                .arrears(expected.subtract(paid).max(BigDecimal.ZERO))
                .contributionCount(own.size())
                .lastContributionDate(own.isEmpty() ? null : own.get(own.size() - 1).getContributionDate())
                .build();
    }

    /** Monthly: one per calendar month touched by the period. Weekly: one per started week. */
    private int contributionsDue(ContributionFrequency frequency, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return 0;
        }
        if (frequency == ContributionFrequency.WEEKLY) {
            return (int) (ChronoUnit.DAYS.between(from, to) / 7) + 1;
        }
        return (int) ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
    }



    /**
     * Ownership guard: memberId is ALWAYS derived from the authenticated
     * JWT principal, never accepted from the client.
     *
     * ASSUMPTION: SecurityContextHolder's authentication name is the
     * user's email/username, matching CustomUserDetailsService from
     * Sprint 1 — adjust the lookup below if your JwtAuthFilter instead
     * puts memberId directly into the token claims.
     */
    private MemberEntity getCurrentMember(UUID groupId) {
        UserEntity user = currentUserService.get();
        log.info("Member lookup -> userId={}, email={}, groupId={}", user.getUserId(), user.getEmail(), groupId);
        return memberRepository.findByUser_UserIdAndGroup_GroupId(user.getUserId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));
    }
}