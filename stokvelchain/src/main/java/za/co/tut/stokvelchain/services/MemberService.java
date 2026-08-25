package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.MemberResponse;

import java.util.UUID;

public interface MemberService  {
    MemberResponse getMemberById(UUID memberId);
}
