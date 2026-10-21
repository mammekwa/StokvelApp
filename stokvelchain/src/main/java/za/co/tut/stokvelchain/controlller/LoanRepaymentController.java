package za.co.tut.stokvelchain.controlller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tut.stokvelchain.dto.request.LoanRepaymentRequest;
import za.co.tut.stokvelchain.dto.response.LoanRepaymentResponse;
import za.co.tut.stokvelchain.services.LoanRepaymentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/loans/{loanId}/repayments")
@RequiredArgsConstructor
public class LoanRepaymentController {
    private final LoanRepaymentService loanRepaymentService;

    /** Member repays part or all of their own loan; recorded on-chain. */
    @PostMapping
    public ResponseEntity<LoanRepaymentResponse> repay(@PathVariable UUID groupId,
                                                       @PathVariable UUID loanId,
                                                       @Valid @RequestBody LoanRepaymentRequest request) {
        LoanRepaymentResponse response = loanRepaymentService.repay(groupId, loanId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Repayment history for a loan (the borrower or the group admin). */
    @GetMapping
    public ResponseEntity<List<LoanRepaymentResponse>> getRepayments(@PathVariable UUID groupId,
                                                                     @PathVariable UUID loanId) {
        return ResponseEntity.ok(loanRepaymentService.getRepayments(groupId, loanId));
    }
}
