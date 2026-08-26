package za.co.tut.stokvelchain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID userId;

    private String email;

    private String phone;

    private String role;

    private boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;
}
