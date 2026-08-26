package za.co.tut.stokvelchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.tut.stokvelchain.dto.request.LoginRequest;
import za.co.tut.stokvelchain.dto.request.RegisterRequest;
import za.co.tut.stokvelchain.dto.response.AuthResponse;
import za.co.tut.stokvelchain.entity.MemberEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.Role;
import za.co.tut.stokvelchain.exception.DuplicateResourceException;
import za.co.tut.stokvelchain.exception.InvalidCredentialsException;
import za.co.tut.stokvelchain.exception.InvalidReferenceException;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.repository.MemberRepository;
import za.co.tut.stokvelchain.repository.StokvelGroupRepository;
import za.co.tut.stokvelchain.repository.UserRepo;
import za.co.tut.stokvelchain.security.JwtUtil;
import za.co.tut.stokvelchain.services.impl.AuthServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepo userRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StokvelGroupRepository stokvelGroupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();

        registerRequest = RegisterRequest.builder()
                .fullName("Thabo Mokoena")
                .email("thabo@example.com")
                .phone("+27821234567")
                .password("password123")
                .nationalId("9001015800081")
                .groupId(groupId)
                .build();

        loginRequest = LoginRequest.builder()
                .email("thabo@example.com")
                .password("password123")
                .build();
    }

    @Test
    void register_shouldReturnAuthResponse_whenRequestIsValid() {
        StokvelGroupEntity group = StokvelGroupEntity.builder().groupId(groupId).build();
        UserEntity savedUser = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .passwordHash("hashed")
                .role(Role.MEMBER)
                .isActive(true)
                .build();
        MemberEntity savedMember = MemberEntity.builder()
                .memberId(UUID.randomUUID())
                .fullName(registerRequest.getFullName())
                .nationalId(registerRequest.getNationalId())
                .user(savedUser)
                .group(group)
                .build();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(false);
        when(stokvelGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(memberRepository.save(any(MemberEntity.class))).thenReturn(savedMember);
        when(jwtUtil.generateToken(any(), anyString(), anyString())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getMemberId()).isEqualTo(savedMember.getMemberId());
        assertThat(response.getRole()).isEqualTo("MEMBER");
        verify(userRepository).save(any(UserEntity.class));
        verify(memberRepository).save(any(MemberEntity.class));
    }

    @Test
    void register_shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowDuplicateResourceException_whenPhoneAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_shouldThrowInvalidReferenceException_whenGroupIdDoesNotExist() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(false);
        when(stokvelGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(InvalidReferenceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email(loginRequest.getEmail())
                .passwordHash("hashed")
                .role(Role.MEMBER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        when(jwtUtil.generateToken(any(), anyString(), anyString())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getRole()).isEqualTo("MEMBER");
    }

    @Test
    void login_shouldThrowResourceNotFoundException_whenAccountDoesNotExist() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenPasswordIsWrong() {
        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email(loginRequest.getEmail())
                .passwordHash("hashed")
                .role(Role.MEMBER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(any());
    }
}
