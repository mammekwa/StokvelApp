package za.co.tut.stokvelchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.tut.stokvelchain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    UUID notificationId;
    NotificationType type;
    String title;
    String message;
    UUID relatedRecordId;
    UUID groupId;
    boolean read;
    LocalDateTime createdAt;
}