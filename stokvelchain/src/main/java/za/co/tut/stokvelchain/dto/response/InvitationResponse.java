package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    UUID invitationId;
    String email;
    InvitationStatus status;
    LocalDateTime expiresAt;
}
