package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.PayoutPreviewResponse;
import za.co.tut.stokvelchain.dto.response.PayoutResponse;

import java.util.List;
import java.util.UUID;

public interface PayoutService {
    PayoutPreviewResponse previewNextPayout(UUID groupId);

    PayoutResponse processNextPayout(UUID groupId);

    PayoutResponse retryPayout(UUID groupId, UUID payoutId);

    List<PayoutResponse> getGroupPayouts(UUID groupId);
}
