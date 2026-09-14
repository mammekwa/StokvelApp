package za.co.tut.stokvelchain.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import za.co.tut.stokvelchain.blockchain.StokvelLedgerContract;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import za.co.tut.stokvelchain.dto.response.BlockchainTestResponse;
import za.co.tut.stokvelchain.exception.BlockchainOperationException;
import za.co.tut.stokvelchain.services.BlockchainService;

import java.math.BigInteger;
import java.util.UUID;

@Slf4j
@Service
public class BlockchainServiceImpl implements BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    public BlockchainServiceImpl(Web3j web3j, Credentials credentials) {
        this.web3j = web3j;
        this.credentials = credentials;
    }

    private StokvelLedgerContract loadContract() {
        return StokvelLedgerContract.load(
                contractAddress,
                web3j,
                credentials,
                new DefaultGasProvider()
        );
    }

    @Override
    public String recordTransaction(BigInteger eventType, BigInteger amount, String memberAddress, String sourceRecordId) {
        try {
            StokvelLedgerContract contract = loadContract();
            TransactionReceipt receipt = contract
                    .recordTransaction(eventType, amount, memberAddress, sourceRecordId)
                    .send();
            log.info("Recorded on-chain transaction: eventType={}, sourceRecordId={}, txHash={}",
                    eventType, sourceRecordId, receipt.getTransactionHash());
            return receipt.getTransactionHash();
        } catch (Exception e) {
            log.error("Failed to record on-chain transaction for sourceRecordId={}", sourceRecordId, e);
            throw new BlockchainOperationException(
                    "Failed to write transaction to StokvelLedger: " + e.getMessage(), e);
        }
    }

    @Override
    public BlockchainTestResponse recordTestTransaction() {
        String testSourceRecordId = "test-" + UUID.randomUUID();
        BigInteger testEventType = BigInteger.ZERO;
        String txHash = recordTransaction(
                testEventType,
                BigInteger.ONE,
                credentials.getAddress(),
                testSourceRecordId
        );

        return BlockchainTestResponse.builder()
                .txHash(txHash)
                .contractAddress(contractAddress)
                .eventType("CONTRIBUTION (0) — test scaffolding, no dedicated TEST case on-chain")
                .sourceRecordId(testSourceRecordId)
                .message("Verify this txHash on Sepolia Etherscan: https://sepolia.etherscan.io/tx/" + txHash)
                .build();
    }
}
