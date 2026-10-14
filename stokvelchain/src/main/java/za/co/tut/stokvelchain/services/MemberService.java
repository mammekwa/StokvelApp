package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.dto.response.MembershipResponse;
import za.co.tut.stokvelchain.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface MemberService  {
    MemberResponse getMemberById(UUID memberId);
    List<MembershipResponse> getMyMemberships();
}
