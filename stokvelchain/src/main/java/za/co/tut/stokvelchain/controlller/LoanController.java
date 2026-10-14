package za.co.tut.stokvelchain.controlller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.dto.request.LoanDecisionRequest;
import za.co.tut.stokvelchain.dto.response.LoanResponse;
import za.co.tut.stokvelchain.enums.LoanStatus;
import za.co.tut.stokvelchain.services.LoanService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    /** Member applies for a loan; it is scored immediately by the credit model. */
    @PostMapping
    public ResponseEntity<LoanResponse> applyForLoan(@PathVariable UUID groupId,
                                                     @Valid @RequestBody LoanApplicationRequest request) {
        LoanResponse response = loanService.applyForLoan(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Member's own loans in this group. */
    @GetMapping("/me")
    public ResponseEntity<List<LoanResponse>> getMyLoans(@PathVariable UUID groupId) {
        return ResponseEntity.ok(loanService.getMyLoans(groupId));
    }

    /** Admin: all loans in the group, optionally filtered, e.g. ?status=PENDING. */
    @GetMapping
    public ResponseEntity<List<LoanResponse>> getGroupLoans(@PathVariable UUID groupId,
                                                            @RequestParam(required = false) LoanStatus status) {
        return ResponseEntity.ok(loanService.getGroupLoans(groupId, status));
    }

    /** Admin: approve or reject; overriding the model requires a category and reason. */
    @PostMapping("/{loanId}/decision")
    public ResponseEntity<LoanResponse> decideLoan(@PathVariable UUID groupId,
                                                   @PathVariable UUID loanId,
                                                   @Valid @RequestBody LoanDecisionRequest request) {
        return ResponseEntity.ok(loanService.decideLoan(groupId, loanId, request));
    }

    /** Admin: retry the on-chain disbursement of an approved loan. */
    @PostMapping("/{loanId}/disbursement/retry")
    public ResponseEntity<LoanResponse> retryDisbursement(@PathVariable UUID groupId,
                                                          @PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.retryDisbursement(groupId, loanId));
    }
}

