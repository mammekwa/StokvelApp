package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import za.co.tut.stokvelchain.util.InviteCodeGenerator;
import za.co.tut.stokvelchain.dto.request.CreateGroupRequest;
import za.co.tut.stokvelchain.dto.request.EmailInviteRequest;
import za.co.tut.stokvelchain.dto.response.*;
import za.co.tut.stokvelchain.entity.GroupInvitationEntity;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.InvitationStatus;
import za.co.tut.stokvelchain.enums.MemberStatus;
import za.co.tut.stokvelchain.repository.GroupInvitationRepository;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.repository.StokvelGroupRepository;
import za.co.tut.stokvelchain.repository.UserRepo;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.EmailService;
import za.co.tut.stokvelchain.services.GroupService;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImpl implements GroupService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final StokvelGroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final GroupInvitationRepository invitationRepository;
    private final UserRepo userRepository;
    private final CurrentUserService currentUserService;
    private final InviteCodeGenerator codeGenerator;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.invitations.expiry-days:7}")
    private int invitationExpiryDays;

    // ── Group creation & viewing ─────────────────────────────────────────────

    @Override
    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        UserEntity user = currentUserService.get();

        StokvelGroupEntity group = StokvelGroupEntity.builder()
                .groupName(request.getGroupName().trim())
                .payoutCycle(request.getPayoutCycle())
                .contributionAmount(request.getContributionAmount())
                .contributionFrequency(request.getContributionFrequency())
                .adminUser(user)
                .inviteCode(newUniqueInviteCode())
                .build();
        group = groupRepository.save(group);

        // The creator is also a normal contributing member, first in the payout rotation.
        MemberEntity adminMember = newMember(user, group, MemberStatus.ACTIVE, 1);
        adminMember.setWalletAddress(generateWalletAddress());
        memberRepository.save(adminMember);

        return toGroupResponse(group, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreateGroupResponse> getMyGroups() {
        UserEntity user = currentUserService.get();
        return memberRepository.findByUserAndStatus(user, MemberStatus.ACTIVE).stream()
                .map(m -> toGroupResponse(m.getGroup(), user))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CreateGroupResponse getGroup(UUID groupId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireActiveMember(group, user);
        return toGroupResponse(group, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getActiveMembers(UUID groupId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireActiveMember(group, user);
        return memberRepository.findByGroupAndStatusOrderByPayoutPositionAsc(group, MemberStatus.ACTIVE).stream()
                .map(m -> toMemberResponse(m, group))
                .toList();
    }

    // ── Invite code / link ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InviteLinkResponse getInviteLink(UUID groupId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireActiveMember(group, user); // any active member may share the link
        return toInviteLink(group);
    }

    @Override
    public InviteLinkResponse regenerateInviteCode(UUID groupId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, user);
        group.setInviteCode(newUniqueInviteCode()); // old links stop working immediately
        return toInviteLink(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPreviewResponse previewByInviteCode(String inviteCode) {
        StokvelGroupEntity group = findGroupByCode(inviteCode);
        return GroupPreviewResponse.builder()
                .groupName(group.getGroupName())
                .contributionAmount(group.getContributionAmount())
                .contributionFrequency(group.getContributionFrequency())
                .payoutCycle(group.getPayoutCycle())
                .activeMemberCount(memberRepository.countByGroupAndStatus(group, MemberStatus.ACTIVE))
                .build();
    }

    @Override
    public JoinResponse joinByInviteCode(String inviteCode) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroupByCode(inviteCode);

        MemberEntity member = createOrReactivateMembership(user, group, MemberStatus.PENDING);
        return JoinResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .status(member.getStatus())
                .message("Your request to join has been sent to the group admin.")
                .build();
    }

    // ── Email invitations ────────────────────────────────────────────────────

    @Override
    public InvitationResponse sendEmailInvitation(UUID groupId, EmailInviteRequest request) {
        UserEntity inviter = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireActiveMember(group, inviter);

        String email = request.getEmail().trim().toLowerCase();

        // Don't invite someone who is already in the group.
        userRepository.findByEmail(email)
                .flatMap(existing -> memberRepository.findByUserAndGroup(existing, group))
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .ifPresent(m -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This person is already a member of the group");
                });

        // Re-inviting the same email replaces the previous invitation.
        invitationRepository.findByGroupAndEmailIgnoreCaseAndStatus(group, email, InvitationStatus.PENDING)
                .forEach(old -> old.setStatus(InvitationStatus.REVOKED));

        GroupInvitationEntity invitation = invitationRepository.save(GroupInvitationEntity.builder()
                .group(group)
                .invitedBy(inviter)
                .email(email)
                .token(codeGenerator.generateInvitationToken())
                .expiresAt(LocalDateTime.now().plusDays(invitationExpiryDays))
                .build());

        emailService.sendGroupInvitation(email, inviter.getFullName(), group.getGroupName(),
                frontendUrl + "/invite/" + invitation.getToken());

        return InvitationResponse.builder()
                .invitationId(invitation.getInvitationId())
                .email(invitation.getEmail())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Override
    public JoinResponse acceptInvitation(String token) {
        UserEntity user = currentUserService.get();
        GroupInvitationEntity invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation is no longer valid");
        }
        if (invitation.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation has expired");
        }
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This invitation was sent to a different email address");
        }

        StokvelGroupEntity group = invitation.getGroup();
        // An invite from the admin counts as approval; an invite from a member still needs it.
        MemberStatus targetStatus = isAdmin(group, invitation.getInvitedBy())
                ? MemberStatus.ACTIVE
                : MemberStatus.PENDING;

        MemberEntity member = createOrReactivateMembership(user, group, targetStatus);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());

        String message = member.getStatus() == MemberStatus.ACTIVE
                ? "You have joined the group."
                : "Your request to join has been sent to the group admin.";
        return JoinResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .status(member.getStatus())
                .message(message)
                .build();
    }

    // ── Admin: approve / reject ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getPendingMembers(UUID groupId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, user);
        return memberRepository.findByGroupAndStatusOrderByJoinedAtAsc(group, MemberStatus.PENDING).stream()
                .map(m -> toMemberResponse(m, group))
                .toList();
    }

    @Override
    public MemberResponse approveMember(UUID groupId, UUID memberId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, user);

        MemberEntity member = findPendingMember(group, memberId);
        member.setStatus(MemberStatus.ACTIVE);
        member.setWalletAddress(generateWalletAddress());

        member.setPayoutPosition(memberRepository.findMaxPayoutPosition(group) + 1);
        return toMemberResponse(member, group);
    }

    @Override
    public MemberResponse rejectMember(UUID groupId, UUID memberId) {
        UserEntity user = currentUserService.get();
        StokvelGroupEntity group = findGroup(groupId);
        requireAdmin(group, user);

        MemberEntity member = findPendingMember(group, memberId);
        member.setStatus(MemberStatus.REJECTED);
        return toMemberResponse(member, group);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * One membership row per (user, group). A user who was rejected or left can request again,
     * which reuses their old row instead of violating the unique constraint.
     */
    private MemberEntity createOrReactivateMembership(UserEntity user, StokvelGroupEntity group, MemberStatus status) {
        Optional<MemberEntity> existing = memberRepository.findByUserAndGroup(user, group);
        Integer position = status == MemberStatus.ACTIVE ? memberRepository.findMaxPayoutPosition(group) + 1 : null;

        if (existing.isEmpty()) {
            return memberRepository.save(newMember(user, group, status, position));
        }

        MemberEntity member = existing.get();
        switch (member.getStatus()) {
            case ACTIVE -> throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group");
            case PENDING -> {
                if (status != MemberStatus.ACTIVE) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Your request to join is already pending");
                }
                // An admin's invitation upgrades an existing pending request.
            }
            case REJECTED, LEFT -> {
                // Allowed to request again: reuse the existing row.
            }
        }
        member.setStatus(status);
        member.setPayoutPosition(position);
        return member;
    }
    private String generateWalletAddress() {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            return Keys.toChecksumAddress(Keys.getAddress(keyPair));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate wallet address", ex);
        }
    }
    private MemberEntity newMember(UserEntity user, StokvelGroupEntity group, MemberStatus status, Integer position) {
        return MemberEntity.builder()
                .fullName(user.getFullName())
                .nationalId(user.getNationalId())
                .user(user)
                .group(group)
                .status(status)
                .payoutPosition(position)
                .build();
    }

    private String newUniqueInviteCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = codeGenerator.generateGroupCode();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code");
    }

    private StokvelGroupEntity findGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private StokvelGroupEntity findGroupByCode(String inviteCode) {
        return groupRepository.findByInviteCode(InviteCodeGenerator.normalize(inviteCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Invalid or expired invite link"));
    }

    private MemberEntity findPendingMember(StokvelGroupEntity group, UUID memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .filter(m -> m.getGroup().getGroupId().equals(group.getGroupId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this group"));
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member is not awaiting approval");
        }
        return member;
    }

    private void requireActiveMember(StokvelGroupEntity group, UserEntity user) {
        memberRepository.findByUserAndGroup(user, group)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group"));
    }

    private void requireAdmin(StokvelGroupEntity group, UserEntity user) {
        if (!isAdmin(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group admin can do this");
        }
    }

    private boolean isAdmin(StokvelGroupEntity group, UserEntity user) {
        // Compare ids, not entities: one side may be a Hibernate proxy.
        return group.getAdminUser().getUserId().equals(user.getUserId());
    }

    private InviteLinkResponse toInviteLink(StokvelGroupEntity group) {
        return InviteLinkResponse.builder()
                .inviteCode(InviteCodeGenerator.format(group.getInviteCode()))
                .inviteLink(frontendUrl + "/join/" + group.getInviteCode())
                .build();
    }

    private CreateGroupResponse toGroupResponse(StokvelGroupEntity group, UserEntity viewer) {
        InviteLinkResponse link = toInviteLink(group);
        return CreateGroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .payoutCycle(group.getPayoutCycle())
                .contributionAmount(group.getContributionAmount())
                .contributionFrequency(group.getContributionFrequency())
                .isAdmin(isAdmin(group, viewer))
                .activeMemberCount(memberRepository.countByGroupAndStatus(group, MemberStatus.ACTIVE))
                .inviteCode(link.getInviteCode())
                .inviteLink(link.getInviteLink())
                .createdAt(group.getCreatedAt())
                .build();
    }

    private MemberResponse toMemberResponse(MemberEntity member, StokvelGroupEntity group) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .fullName(member.getFullName())
                .status(member.getStatus())
                .payoutPosition(member.getPayoutPosition())
                .isAdmin(isAdmin(group, member.getUser()))
                .joinedAt(member.getJoinedAt())
                .build();
    }

}
