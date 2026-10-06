package za.co.tut.stokvelchain.controlller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import za.co.tut.stokvelchain.dto.request.CreateGroupRequest;
import za.co.tut.stokvelchain.dto.request.EmailInviteRequest;
import za.co.tut.stokvelchain.dto.response.*;
import za.co.tut.stokvelchain.services.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ── Groups ───────────────────────────────────────────────────────────────

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    @GetMapping("/groups/mine")
    public List<CreateGroupResponse> myGroups() {
        return groupService.getMyGroups();
    }

    @GetMapping("/groups/{groupId}")
    public CreateGroupResponse getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }

    @GetMapping("/groups/{groupId}/members")
    public List<MemberResponse> members(@PathVariable UUID groupId) {
        return groupService.getActiveMembers(groupId);
    }

    // ── Invite link ──────────────────────────────────────────────────────────

    @GetMapping("/groups/{groupId}/invite-link")
    public InviteLinkResponse inviteLink(@PathVariable UUID groupId) {
        return groupService.getInviteLink(groupId);
    }

    @PostMapping("/groups/{groupId}/invite-code/regenerate")
    public InviteLinkResponse regenerateInviteCode(@PathVariable UUID groupId) {
        return groupService.regenerateInviteCode(groupId);
    }

    /** Public: lets someone see what they are joining before logging in. */
    @GetMapping("/groups/join/{inviteCode}")
    public GroupPreviewResponse preview(@PathVariable String inviteCode) {
        return groupService.previewByInviteCode(inviteCode);
    }

    @PostMapping("/groups/join/{inviteCode}")
    public JoinResponse join(@PathVariable String inviteCode) {
        return groupService.joinByInviteCode(inviteCode);
    }

    // ── Email invitations ────────────────────────────────────────────────────

    @PostMapping("/groups/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@PathVariable UUID groupId, @Valid @RequestBody EmailInviteRequest request) {
        return groupService.sendEmailInvitation(groupId, request);
    }

    @PostMapping("/invitations/{token}/accept")
    public JoinResponse acceptInvitation(@PathVariable String token) {
        return groupService.acceptInvitation(token);
    }

    // ── Admin: membership requests ───────────────────────────────────────────

    @GetMapping("/groups/{groupId}/members/pending")
    public List<MemberResponse> pending(@PathVariable UUID groupId) {
        return groupService.getPendingMembers(groupId);
    }

    @PostMapping("/groups/{groupId}/members/{memberId}/approve")
    public MemberResponse approve(@PathVariable UUID groupId, @PathVariable UUID memberId) {
        return groupService.approveMember(groupId, memberId);
    }

    @PostMapping("/groups/{groupId}/members/{memberId}/reject")
    public MemberResponse reject(@PathVariable UUID groupId, @PathVariable UUID memberId) {
        return groupService.rejectMember(groupId, memberId);
    }
}
