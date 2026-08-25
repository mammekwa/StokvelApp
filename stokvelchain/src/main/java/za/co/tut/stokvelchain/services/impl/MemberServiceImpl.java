package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.mapper.MemberMapper;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.services.MemberService;

import java.util.UUID;
@Service
@RequiredArgsConstructor

public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(UUID memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

        return memberMapper.toMemberResponse(member);
    }
}
