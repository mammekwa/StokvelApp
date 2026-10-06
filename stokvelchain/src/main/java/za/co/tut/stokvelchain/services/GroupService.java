package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.request.CreateGroupRequest;
import za.co.tut.stokvelchain.dto.request.EmailInviteRequest;
import za.co.tut.stokvelchain.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface GroupService {
    CreateGroupResponse createGroup(CreateGroupRequest request);

    List<CreateGroupResponse> getMyGroups();

    CreateGroupResponse getGroup(UUID groupId);

    List<MemberResponse> getActiveMembers(UUID groupId);

    InviteLinkResponse getInviteLink(UUID groupId);

    InviteLinkResponse regenerateInviteCode(UUID groupId);

    GroupPreviewResponse previewByInviteCode(String inviteCode);

    JoinResponse joinByInviteCode(String inviteCode);

    InvitationResponse sendEmailInvitation(UUID groupId, EmailInviteRequest request);

    JoinResponse acceptInvitation(String token);

    List<MemberResponse> getPendingMembers(UUID groupId);

    MemberResponse approveMember(UUID groupId, UUID memberId);

    MemberResponse rejectMember(UUID groupId, UUID memberId);
}
