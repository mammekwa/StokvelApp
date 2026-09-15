package za.co.tut.stokvelchain.blockchain;

import za.co.tut.stokvelchain.enums.EventType;

import java.math.BigInteger;

public final class EventTypeChainMapper {
    private EventTypeChainMapper() {}

    public static BigInteger toChainOrdinal(EventType eventType) {
        return switch (eventType) {
            case CONTRIBUTION -> BigInteger.valueOf(0);
            case PAYOUT -> BigInteger.valueOf(1);
            case LOAN_DISBURSEMENT -> BigInteger.valueOf(2);
            case LOAN_REPAYMENT -> BigInteger.valueOf(3);
        };
    }
}
