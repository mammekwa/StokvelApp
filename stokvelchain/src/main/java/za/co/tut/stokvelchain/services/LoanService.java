package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.LoanApplicationRequest;
import za.co.tut.stokvelchain.dto.request.LoanDecisionRequest;
import za.co.tut.stokvelchain.dto.response.LoanResponse;
import za.co.tut.stokvelchain.enums.LoanStatus;

import java.util.List;
import java.util.UUID;

public interface LoanService {
    LoanResponse applyForLoan(UUID groupId, LoanApplicationRequest request);

    List<LoanResponse> getMyLoans(UUID groupId);

    List<LoanResponse> getGroupLoans(UUID groupId, LoanStatus status);

    LoanResponse decideLoan(UUID groupId, UUID loanId, LoanDecisionRequest request);

    LoanResponse retryDisbursement(UUID groupId, UUID loanId);
}