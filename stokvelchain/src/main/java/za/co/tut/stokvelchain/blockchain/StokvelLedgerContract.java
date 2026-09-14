package za.co.tut.stokvelchain.blockchain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

public class StokvelLedgerContract extends Contract {
    public static final String BINARY = "608060405234801561000f575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555061107a8061005c5f395ff3fe608060405234801561000f575f80fd5b506004361061004a575f3560e01c80637fef036e1461004e5780638da5cb5b1461006c578063bae78d7b1461008a578063ead95d37146100ba575b5f80fd5b6100566100ea565b6040516100639190610653565b60405180910390f35b6100746100f6565b60405161008191906106ab565b60405180910390f35b6100a4600480360381019061009f91906106f6565b610119565b6040516100b191906108af565b60405180910390f35b6100d460048036038101906100cf919061097d565b6102cc565b6040516100e19190610653565b60405180910390f35b5f600180549050905090565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6101216105e8565b6001805490508210610168576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161015f90610a81565b60405180910390fd5b6001828154811061017c5761017b610a9f565b5b905f5260205f2090600502016040518060a00160405290815f82015f9054906101000a900460ff1660038111156101b6576101b5610721565b5b60038111156101c8576101c7610721565b5b815260200160018201548152602001600282015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200160038201805461023b90610af9565b80601f016020809104026020016040519081016040528092919081815260200182805461026790610af9565b80156102b25780601f10610289576101008083540402835291602001916102b2565b820191905f5260205f20905b81548152906001019060200180831161029557829003601f168201915b505050505081526020016004820154815250509050919050565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461035b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161035290610b99565b60405180910390fd5b5f851161039d576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161039490610c27565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff160361040b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161040290610cb5565b60405180910390fd5b60016040518060a0016040528088600381111561042b5761042a610721565b5b81526020018781526020018673ffffffffffffffffffffffffffffffffffffffff16815260200185858080601f0160208091040260200160405190810160405280939291908181526020018383808284375f81840152601f19601f82011690508083019250505050505050815260200142815250908060018154018082558091505060019003905f5260205f2090600502015f909190919091505f820151815f015f6101000a81548160ff021916908360038111156104ed576104ec610721565b5b0217905550602082015181600101556040820151816002015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060608201518160030190816105579190610e9d565b50608082015181600401555050600180805490506105759190610f99565b90508373ffffffffffffffffffffffffffffffffffffffff168660038111156105a1576105a0610721565b5b827f1f67a035762d156dbb0fa49c912079cf8e4b835c5c2cc170163d1bd2ed8e45b9888787426040516105d79493929190611006565b60405180910390a495945050505050565b6040518060a001604052805f600381111561060657610605610721565b5b81526020015f81526020015f73ffffffffffffffffffffffffffffffffffffffff168152602001606081526020015f81525090565b5f819050919050565b61064d8161063b565b82525050565b5f6020820190506106665f830184610644565b92915050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6106958261066c565b9050919050565b6106a58161068b565b82525050565b5f6020820190506106be5f83018461069c565b92915050565b5f80fd5b5f80fd5b6106d58161063b565b81146106df575f80fd5b50565b5f813590506106f0816106cc565b92915050565b5f6020828403121561070b5761070a6106c4565b5b5f610718848285016106e2565b91505092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602160045260245ffd5b6004811061075f5761075e610721565b5b50565b5f81905061076f8261074e565b919050565b5f61077e82610762565b9050919050565b61078e81610774565b82525050565b61079d8161063b565b82525050565b6107ac8161068b565b82525050565b5f81519050919050565b5f82825260208201905092915050565b5f5b838110156107e95780820151818401526020810190506107ce565b5f8484015250505050565b5f601f19601f8301169050919050565b5f61080e826107b2565b61081881856107bc565b93506108288185602086016107cc565b610831816107f4565b840191505092915050565b5f60a083015f8301516108515f860182610785565b5060208301516108646020860182610794565b50604083015161087760408601826107a3565b506060830151848203606086015261088f8282610804565b91505060808301516108a46080860182610794565b508091505092915050565b5f6020820190508181035f8301526108c7818461083c565b905092915050565b600481106108db575f80fd5b50565b5f813590506108ec816108cf565b92915050565b6108fb8161068b565b8114610905575f80fd5b50565b5f81359050610916816108f2565b92915050565b5f80fd5b5f80fd5b5f80fd5b5f8083601f84011261093d5761093c61091c565b5b8235905067ffffffffffffffff81111561095a57610959610920565b5b60208301915083600182028301111561097657610975610924565b5b9250929050565b5f805f805f60808688031215610996576109956106c4565b5b5f6109a3888289016108de565b95505060206109b4888289016106e2565b94505060406109c588828901610908565b935050606086013567ffffffffffffffff8111156109e6576109e56106c8565b5b6109f288828901610928565b92509250509295509295909350565b5f82825260208201905092915050565b7f53746f6b76656c4c65646765723a20656e74727920646f6573206e6f742065785f8201527f6973740000000000000000000000000000000000000000000000000000000000602082015250565b5f610a6b602383610a01565b9150610a7682610a11565b604082019050919050565b5f6020820190508181035f830152610a9881610a5f565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52603260045260245ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f6002820490506001821680610b1057607f821691505b602082108103610b2357610b22610acc565b5b50919050565b7f53746f6b76656c4c65646765723a2063616c6c6572206973206e6f74207468655f8201527f206f776e65720000000000000000000000000000000000000000000000000000602082015250565b5f610b83602683610a01565b9150610b8e82610b29565b604082019050919050565b5f6020820190508181035f830152610bb081610b77565b9050919050565b7f53746f6b76656c4c65646765723a20616d6f756e74206d7573742062652067725f8201527f6561746572207468616e207a65726f0000000000000000000000000000000000602082015250565b5f610c11602f83610a01565b9150610c1c82610bb7565b604082019050919050565b5f6020820190508181035f830152610c3e81610c05565b9050919050565b7f53746f6b76656c4c65646765723a20696e76616c6964206d656d6265722061645f8201527f6472657373000000000000000000000000000000000000000000000000000000602082015250565b5f610c9f602583610a01565b9150610caa82610c45565b604082019050919050565b5f6020820190508181035f830152610ccc81610c93565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f60088302610d5c7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff82610d21565b610d668683610d21565b95508019841693508086168417925050509392505050565b5f819050919050565b5f610da1610d9c610d978461063b565b610d7e565b61063b565b9050919050565b5f819050919050565b610dba83610d87565b610dce610dc682610da8565b848454610d2d565b825550505050565b5f90565b610de2610dd6565b610ded818484610db1565b505050565b5b81811015610e1057610e055f82610dda565b600181019050610df3565b5050565b601f821115610e5557610e2681610d00565b610e2f84610d12565b81016020851015610e3e578190505b610e52610e4a85610d12565b830182610df2565b50505b505050565b5f82821c905092915050565b5f610e755f1984600802610e5a565b1980831691505092915050565b5f610e8d8383610e66565b9150826002028217905092915050565b610ea6826107b2565b67ffffffffffffffff811115610ebf57610ebe610cd3565b5b610ec98254610af9565b610ed4828285610e14565b5f60209050601f831160018114610f05575f8415610ef3578287015190505b610efd8582610e82565b865550610f64565b601f198416610f1386610d00565b5f5b82811015610f3a57848901518255600182019150602085019450602081019050610f15565b86831015610f575784890151610f53601f891682610e66565b8355505b6001600288020188555050505b505050505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f610fa38261063b565b9150610fae8361063b565b9250828203905081811115610fc657610fc5610f6c565b5b92915050565b828183375f83830152505050565b5f610fe58385610a01565b9350610ff2838584610fcc565b610ffb836107f4565b840190509392505050565b5f6060820190506110195f830187610644565b818103602083015261102c818587610fda565b905061103b6040830184610644565b9594505050505056fea2646970667358221220beb71d18b45e9cea03eaa0ee7ab81c2f5f585dcd54f65d79563487079a9a600e64736f6c63430008140033";

    public static final String FUNC_GETENTRY = "getEntry";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RECORDTRANSACTION = "recordTransaction";

    public static final String FUNC_TOTALENTRIES = "totalEntries";

    public static final Event TRANSACTIONRECORDED_EVENT = new Event("TransactionRecorded",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},   // entryId (indexed)
                    new TypeReference<Uint8>(true) {},      // eventType (indexed)
                    new TypeReference<Uint256>() {},        // amount
                    new TypeReference<Address>(true) {},    // member (indexed)
                    new TypeReference<Utf8String>() {},     // sourceRecordId
                    new TypeReference<Uint256>() {}         // timestamp
            ));

    protected StokvelLedgerContract(String contractAddress, Web3j web3j, Credentials credentials,
                                    ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected StokvelLedgerContract(String contractAddress, Web3j web3j, TransactionManager transactionManager,
                                    ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static StokvelLedgerContract load(String contractAddress, Web3j web3j, Credentials credentials,
                                             ContractGasProvider contractGasProvider) {
        return new StokvelLedgerContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static StokvelLedgerContract load(String contractAddress, Web3j web3j,
                                             TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new StokvelLedgerContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    /**
     * @param eventType 0=CONTRIBUTION, 1=PAYOUT, 2=LOAN_DISBURSEMENT, 3=LOAN_REPAYMENT
     */
    public RemoteFunctionCall<TransactionReceipt> recordTransaction(BigInteger eventType, BigInteger amount,
                                                                    String member, String sourceRecordId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RECORDTRANSACTION,
                Arrays.asList(
                        new Uint8(eventType),
                        new Uint256(amount),
                        new Address(member),
                        new Utf8String(sourceRecordId)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<LedgerEntry> getEntry(BigInteger entryId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETENTRY,
                Arrays.asList(new Uint256(entryId)),
                Arrays.asList(new TypeReference<LedgerEntry>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<BigInteger> totalEntries() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TOTALENTRIES,
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_OWNER,
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public List<TransactionRecordedEventResponse> getTransactionRecordedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSACTIONRECORDED_EVENT,
                transactionReceipt);
        ArrayList<TransactionRecordedEventResponse> responses = new ArrayList<>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransactionRecordedEventResponse typedResponse = new TransactionRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.entryId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.eventType = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.member = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.sourceRecordId = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransactionRecordedEventResponse> transactionRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> {
            Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSACTIONRECORDED_EVENT, log);
            TransactionRecordedEventResponse typedResponse = new TransactionRecordedEventResponse();
            typedResponse.log = log;
            typedResponse.entryId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.eventType = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.member = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.sourceRecordId = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            return typedResponse;
        });
    }

    /**
     * Mirrors the Solidity LedgerEntry struct returned by getEntry(). It's a
     * DynamicStruct (not a StaticStruct) because sourceRecordId is a string,
     * a variable-length type.
     */
    public static class LedgerEntry extends DynamicStruct {
        public BigInteger eventType;
        public BigInteger amount;
        public String member;
        public String sourceRecordId;
        public BigInteger timestamp;

        public LedgerEntry(BigInteger eventType, BigInteger amount, String member, String sourceRecordId,
                           BigInteger timestamp) {
            super(new Uint8(eventType), new Uint256(amount), new Address(member), new Utf8String(sourceRecordId),
                    new Uint256(timestamp));
            this.eventType = eventType;
            this.amount = amount;
            this.member = member;
            this.sourceRecordId = sourceRecordId;
            this.timestamp = timestamp;
        }

        public LedgerEntry(Uint8 eventType, Uint256 amount, Address member, Utf8String sourceRecordId,
                           Uint256 timestamp) {
            super(eventType, amount, member, sourceRecordId, timestamp);
            this.eventType = eventType.getValue();
            this.amount = amount.getValue();
            this.member = member.getValue();
            this.sourceRecordId = sourceRecordId.getValue();
            this.timestamp = timestamp.getValue();
        }
    }

    public static class TransactionRecordedEventResponse extends BaseEventResponse {
        public BigInteger entryId;
        public BigInteger eventType;
        public BigInteger amount;
        public String member;
        public String sourceRecordId;
        public BigInteger timestamp;
    }
}
