package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import za.co.tut.stokvelchain.services.AuthService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepo userRepository;
    private final MemberRepository memberRepository;
    private final StokvelGroupRepository stokvelGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("An account with this phone number already exists");
        }

        StokvelGroupEntity group = stokvelGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new InvalidReferenceException("groupId does not reference an existing stokvel group"));

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.MEMBER)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        MemberEntity member = MemberEntity.builder()
                .fullName(request.getFullName())
                .nationalId(request.getNationalId())
                .loanRestricted(false)
                .user(user)
                .group(group)
                .build();
        member = memberRepository.save(member);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .memberId(member.getMemberId())
                .token(token)
                .role(user.getRole().name())
                .build();
    }


    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect password");
        }

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .build();
    }
}
