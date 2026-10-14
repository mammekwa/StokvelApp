package za.co.tut.stokvelchain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.OverrideReason;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecisionRequest {
    /** true = approve, false = reject. */
    @NotNull(message = "A decision (approve true/false) is required")
    Boolean approve;

    /** Required only when the decision differs from the credit model's recommendation. */
    OverrideReason overrideCategory;

    /** Required (min 20 characters) only when overriding the credit model. */
    @Size(max = 1000, message = "Override reason cannot exceed 1000 characters")
    String overrideReason;
}
