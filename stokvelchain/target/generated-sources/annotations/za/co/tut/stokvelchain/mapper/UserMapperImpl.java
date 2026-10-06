package za.co.tut.stokvelchain.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import za.co.tut.stokvelchain.dto.response.UserResponse;
import za.co.tut.stokvelchain.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-29T21:18:06+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(UserEntity userEntity) {
        if ( userEntity == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.userId( userEntity.getUserId() );
        userResponse.email( userEntity.getEmail() );
        userResponse.phone( userEntity.getPhone() );
        userResponse.createdAt( userEntity.getCreatedAt() );
        userResponse.lastLoginAt( userEntity.getLastLoginAt() );

        userResponse.role( userEntity.getRole() != null ? userEntity.getRole().name() : null );

        return userResponse.build();
    }
}
