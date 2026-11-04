package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRotationEntry {
    UUID memberId;
    String memberName;
    Integer payoutPosition;
    boolean next;
    /** When this member last received a confirmed payout — null if never. */
    LocalDateTime lastPaidAt;
}
