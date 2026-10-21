package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentResponse {
    UUID repaymentId;
    UUID loanId;
    BigDecimal amount;
    LocalDate paymentDate;
    String paymentReference;
    String txHash;
    boolean blockchainConfirmed;

    /** Outstanding balance after this repayment; only set on the repay call. */
    BigDecimal remainingBalance;
}
