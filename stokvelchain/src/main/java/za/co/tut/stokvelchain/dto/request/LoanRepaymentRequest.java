package za.co.tut.stokvelchain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Repayment amount must be greater than zero")
    BigDecimal amount;

    @Size(max = 100, message = "Payment reference cannot exceed 100 characters")
    String paymentReference;
}
