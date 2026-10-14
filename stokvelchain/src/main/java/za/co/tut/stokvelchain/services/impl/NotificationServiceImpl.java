package za.co.tut.stokvelchain.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tut.stokvelchain.dto.response.NotificationResponse;
import za.co.tut.stokvelchain.entity.NotificationEntity;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.NotificationType;
import za.co.tut.stokvelchain.exception.ResourceNotFoundException;
import za.co.tut.stokvelchain.repository.NotificationRepository;
import za.co.tut.stokvelchain.services.CurrentUserService;
import za.co.tut.stokvelchain.services.NotificationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void notify(UserEntity recipient, StokvelGroupEntity group, NotificationType type,
                       String title, String message, UUID relatedRecordId) {
        NotificationEntity notification = NotificationEntity.builder()
                .recipient(recipient)
                .group(group)
                .type(type)
                .title(title)
                .message(message)
                .relatedRecordId(relatedRecordId)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        UserEntity user = currentUserService.get();
        return notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByRecipient_UserIdAndReadFalse(currentUserService.get().getUserId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UserEntity user = currentUserService.get();
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("You can only update your own notifications");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedRecordId(notification.getRelatedRecordId())
                .groupId(notification.getGroup() != null ? notification.getGroup().getGroupId() : null)
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
