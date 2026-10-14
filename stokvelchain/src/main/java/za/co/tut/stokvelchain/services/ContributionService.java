package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;

import java.util.List;
import java.util.UUID;

public interface ContributionService {
    ContributionResponse logContribution(UUID groupId, ContributionRequest request);
    List<ContributionResponse> getMyContributions(UUID groupId);
}
