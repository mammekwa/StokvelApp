package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;

import java.util.List;

public interface ContributionService {
    ContributionResponse logContribution(ContributionRequest request);
    List<ContributionResponse> getMyContributions();
}
