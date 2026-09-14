package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainTestResponse {

    private String txHash;
    private String contractAddress;
    private String eventType;
    private String sourceRecordId;
    private String message;
;
}
