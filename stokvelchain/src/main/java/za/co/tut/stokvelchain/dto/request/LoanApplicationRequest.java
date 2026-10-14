package za.co.tut.stokvelchain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Loan amount must be greater than zero")
    BigDecimal amount;

    @Min(value = 1, message = "Repayment period must be at least 1 month")
    @Max(value = 24, message = "Repayment period cannot exceed 24 months")
    int repaymentMonths;
}
