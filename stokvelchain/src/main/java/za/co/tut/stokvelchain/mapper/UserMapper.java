package za.co.tut.stokvelchain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.tut.stokvelchain.dto.response.UserResponse;
import za.co.tut.stokvelchain.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", expression = "java(userEntity.getRole() != null ? userEntity.getRole().name() : null)")
    UserResponse toUserResponse(UserEntity userEntity);
}
