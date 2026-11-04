package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.ContributionLedgerResponse;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContributionService {
    ContributionResponse logContribution(UUID groupId, ContributionRequest request);
    List<ContributionResponse> getMyContributions(UUID groupId);
    ContributionLedgerResponse getGroupLedger(UUID groupId, LocalDate from, LocalDate to);
}

