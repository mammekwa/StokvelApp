package za.co.tut.stokvelchain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tut.stokvelchain.dto.response.UserResponse;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.Role;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.mapper.UserMapper;
import za.co.tut.stokvelchain.repository.UserRepo;
import za.co.tut.stokvelchain.services.impl.UserServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepo userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUserById_shouldReturnUserResponse_whenUserExists() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .userId(userId)
                .email("thabo@example.com")
                .phone("+27821234567")
                .role(
                        Role.MEMBER)
                .isActive(true)
                .build();
        UserResponse expected = UserResponse.builder()
                .userId(userId)
                .email("thabo@example.com")
                .phone("+27821234567")
                .role("MEMBER")
                .isActive(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expected);

        UserResponse result = userService.getUserById(userId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getUserById_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
