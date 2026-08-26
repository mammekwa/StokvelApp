package za.co.tut.stokvelchain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.tut.stokvelchain.dto.response.MemberResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;

@Mapper(componentModel = "spring")
public interface MemberMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "groupId", source = "group.groupId")
    @Mapping(target = "groupName", source = "group.groupName")
    MemberResponse toMemberResponse(MemberEntity memberEntity);
}
