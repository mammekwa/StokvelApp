package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.dto.response.MembershipResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.mapper.MemberMapper;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.MemberService;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(UUID memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        return memberMapper.toMemberResponse(member);
    }
    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> getMyMemberships() {
        UserEntity user = currentUserService.get();
        return memberRepository.findByUser_UserId(user.getUserId())
                .stream()
                .map(m -> MembershipResponse.builder()
                        .memberId(m.getMemberId())
                        .groupId(m.getGroup().getGroupId())
                        .groupName(m.getGroup().getGroupName())
                        .status(m.getStatus().name())
                        .build())
                .toList();
    }
    private MemberEntity getCurrentMember(UUID groupId) {
        UserEntity user = currentUserService.get();
        log.info("Member lookup -> userId={}, email={}, groupId={}", user.getUserId(), user.getEmail(), groupId);
        return memberRepository.findByUser_UserIdAndGroup_GroupId(user.getUserId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group"));
    }
}
