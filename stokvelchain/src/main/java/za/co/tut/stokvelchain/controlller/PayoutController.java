package za.co.tut.stokvelchain.controlller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tut.stokvelchain.dto.response.PayoutPreviewResponse;
import za.co.tut.stokvelchain.dto.response.PayoutResponse;
import za.co.tut.stokvelchain.services.PayoutService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/payouts")
@RequiredArgsConstructor
public class PayoutController {
    private final PayoutService payoutService;

    /** Who is next, how much is available, and the rotation order (any group member). */
    @GetMapping("/next")
    public ResponseEntity<PayoutPreviewResponse> previewNextPayout(@PathVariable UUID groupId) {
        return ResponseEntity.ok(payoutService.previewNextPayout(groupId));
    }

    /** Admin: pay the group balance to the next member in the rotation; recorded on-chain. */
    @PostMapping
    public ResponseEntity<PayoutResponse> processNextPayout(@PathVariable UUID groupId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payoutService.processNextPayout(groupId));
    }

    /** Admin: retry the on-chain recording of an unconfirmed payout. */
    @PostMapping("/{payoutId}/retry")
    public ResponseEntity<PayoutResponse> retryPayout(@PathVariable UUID groupId, @PathVariable UUID payoutId) {
        return ResponseEntity.ok(payoutService.retryPayout(groupId, payoutId));
    }

    /** Payout history for the group (any group member). */
    @GetMapping
    public ResponseEntity<List<PayoutResponse>> getGroupPayouts(@PathVariable UUID groupId) {
        return ResponseEntity.ok(payoutService.getGroupPayouts(groupId));
    }
}

