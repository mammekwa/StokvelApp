package za.co.tut.stokvelchain.controlller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tut.stokvelchain.dto.request.ContributionRequest;
import za.co.tut.stokvelchain.dto.response.ContributionResponse;
import za.co.tut.stokvelchain.services.ContributionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
public class ContributionController {
    private final ContributionService contributionService;

    @PostMapping
    public ResponseEntity<ContributionResponse> logContribution(@Valid @RequestBody ContributionRequest request) {
        ContributionResponse response = contributionService.logContribution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ContributionResponse>> getMyContributions() {
        return ResponseEntity.ok(contributionService.getMyContributions());
    }
}
