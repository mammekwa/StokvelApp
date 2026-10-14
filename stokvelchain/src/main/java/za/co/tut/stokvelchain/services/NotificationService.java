package za.co.tut.stokvelchain.services;

import za.co.tut.stokvelchain.dto.response.NotificationResponse;
import za.co.tut.stokvelchain.entity.StokvelGroupEntity;
import za.co.tut.stokvelchain.entity.UserEntity;
import za.co.tut.stokvelchain.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void notify(UserEntity recipient, StokvelGroupEntity group, NotificationType type,
                String title, String message, UUID relatedRecordId);

    List<NotificationResponse> getMyNotifications();

    long getUnreadCount();

    NotificationResponse markAsRead(UUID notificationId);
}