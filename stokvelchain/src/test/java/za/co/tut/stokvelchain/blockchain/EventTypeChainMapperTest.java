package za.co.tut.stokvelchain.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import za.co.tut.stokvelchain.enums.EventType;

import java.math.BigInteger;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EventTypeChainMapperTest {

    @ParameterizedTest(name = "{0} -> on-chain {1}")
    @CsvSource({
            "CONTRIBUTION,      0",
            "PAYOUT,            1",
            "LOAN_DISBURSEMENT, 2",
            "LOAN_REPAYMENT,    3"
    })
    @DisplayName("Each event type matches the smart contract's enum order")
    void mapsToContractOrdinal(EventType type, int expected) {
        assertThat(EventTypeChainMapper.toChainOrdinal(type)).isEqualTo(BigInteger.valueOf(expected));
    }

    @Test
    @DisplayName("Every event type has an on-chain value, and no two share one")
    void everyEventTypeIsMappedUniquely() {
        for (EventType type : EventType.values()) {
            assertThatCode(() -> EventTypeChainMapper.toChainOrdinal(type)).doesNotThrowAnyException();
        }
        long distinct = Arrays.stream(EventType.values()).map(EventTypeChainMapper::toChainOrdinal).distinct().count();
        assertThat(distinct).isEqualTo(EventType.values().length);
    }
}
