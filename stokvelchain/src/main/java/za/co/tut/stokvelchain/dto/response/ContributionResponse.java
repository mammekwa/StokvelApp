package za.co.tut.stokvelchain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ContributionResponse {
    private UUID contributionId;
    private UUID memberId;
    private BigDecimal amount;
    private LocalDate contributionDate;
    private String txHash;
    private boolean blockchainConfirmed;
    private LocalDateTime createdAt;
}
