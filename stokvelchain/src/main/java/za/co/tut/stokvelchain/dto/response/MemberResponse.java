package za.co.tut.stokvelchain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private UUID memberId;

    private String fullName;

    private String nationalId;

    private boolean loanRestricted;

    private LocalDateTime joinedAt;

    private UUID userId;

    private UUID groupId;

    private String groupName;
}
