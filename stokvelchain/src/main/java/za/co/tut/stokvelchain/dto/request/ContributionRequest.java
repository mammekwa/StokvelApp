package za.co.tut.stokvelchain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ContributionRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "Contribution amount must be greater than zero")
    private BigDecimal amount;

    // Optional client-supplied reference (e.g. bank ref); not required.
    private String paymentReference;
}
