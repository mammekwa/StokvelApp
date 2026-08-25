package za.co.tut.stokvelchain.controlller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.services.MemberService;

import java.util.UUID;
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }
}
