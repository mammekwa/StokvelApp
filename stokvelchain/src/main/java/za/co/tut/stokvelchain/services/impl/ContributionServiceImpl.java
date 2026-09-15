package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tut.stokvelchain.blockchain.EventTypeChainMapper;
import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;
import za.co.tut.stokvelchain.entity.ContributionEntity;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.TransactionEntity;
import za.co.tut.stokvelchain.enums.EventType;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.mapper.ContributionMapper;
import za.co.tut.stokvelchain.repository.ContributionRepository;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.repository.TransactionRepository;
import za.co.tut.stokvelchain.services.BlockchainService;
import za.co.tut.stokvelchain.services.ContributionService;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContributionServiceImpl implements ContributionService {
    private final ContributionRepository contributionRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final BlockchainService blockchainService;
    private final ContributionMapper contributionMapper;

    @Override
    @Transactional
    public ContributionResponse logContribution(ContributionRequest request) {
        MemberEntity member = getCurrentMember();

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
            throw new BlockchainOperationException(
                    "Failed to record contribution on-chain for id " + contribution.getContributionId(), ex);
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

    @Override
    public List<ContributionResponse> getMyContributions() {
        MemberEntity member = getCurrentMember();
        return contributionRepository.findByMember_MemberId(member.getMemberId())
                .stream()
                .map(contributionMapper::toResponse)
                .toList();
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
    private MemberEntity getCurrentMember() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByUser_Email(username)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found for user: " + username));
    }
}
