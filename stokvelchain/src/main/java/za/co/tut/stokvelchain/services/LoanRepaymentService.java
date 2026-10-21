package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.LoanRepaymentRequest;
import za.co.tut.stokvelchain.dto.response.LoanRepaymentResponse;

import java.util.List;
import java.util.UUID;

public interface LoanRepaymentService {
    LoanRepaymentResponse repay(UUID groupId, UUID loanId, LoanRepaymentRequest request);

    List<LoanRepaymentResponse> getRepayments(UUID groupId, UUID loanId);
}
