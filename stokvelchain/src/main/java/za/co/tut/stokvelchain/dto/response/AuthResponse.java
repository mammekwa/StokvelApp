package za.co.tut.stokvelchain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private UUID memberId;

    private String token;

    private String role;
}
