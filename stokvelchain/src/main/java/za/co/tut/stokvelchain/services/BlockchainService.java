package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.BlockchainTestResponse;

import java.math.BigInteger;

public interface BlockchainService {
    /**
     * Writes an entry to the on-chain StokvelLedger and returns the
     * resulting transaction hash. This is the method future features
     * (contributions, loans, payouts) will call directly once wired
     * up in Sprint 3+.
     *
     * @param eventType      ordinal of the on-chain EventType enum: 0=CONTRIBUTION, 1=PAYOUT, 2=LOAN_DISBURSEMENT, 3=LOAN_REPAYMENT. If your domain-layer EventType enum (used on TransactionEntity) declares its constants in this same order, callers can just pass domainEventType.ordinal() — verify the declaration order matches before relying on that.
     * @param amount         amount in the smallest currency unit (cents), per the contract's NatSpec comment
     * @param memberAddress  the member's associated wallet address
     * @param sourceRecordId mirrors TransactionEntity's sourceRecordId, for cross-referencing Postgres <-> chain
     * @return the on-chain transaction hash, verifiable on Sepolia Etherscan
     */
    String recordTransaction(BigInteger eventType, BigInteger amount, String memberAddress, String sourceRecordId);

    /**
     * Scaffolding for STOKVEL-203's temporary test endpoint. Writes a
     * throwaway entry to the ledger using fixed test values so the
     * full path (Spring Boot -> Web3j -> Sepolia) can be proven
     * end-to-end without needing a real member/contribution yet.
     * Remove this method and its controller endpoint once Sprint 3's
     * contribution logging calls recordTransaction(...) directly.
     */
    BlockchainTestResponse recordTestTransaction();
}
