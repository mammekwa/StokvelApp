package za.co.tut.stokvelchain.controlller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.tut.stokvelchain.dto.response.BlockchainTestResponse;
import za.co.tut.stokvelchain.services.BlockchainService;

@RestController
@RequestMapping("/api/v1/blockchain")
@RequiredArgsConstructor
public class BlockchainController {
    private final BlockchainService blockchainService;

    @PostMapping("/test")
    public ResponseEntity<BlockchainTestResponse> testBlockchainWrite() {
        BlockchainTestResponse response = blockchainService.recordTestTransaction();
        return ResponseEntity.ok(response);
    }
}
