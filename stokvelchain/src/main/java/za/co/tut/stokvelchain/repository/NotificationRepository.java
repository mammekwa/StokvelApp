package za.co.tut.stokvelchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tut.stokvelchain.entity.NotificationEntity;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByRecipient_UserIdOrderByCreatedAtDesc(UUID userId);
    long countByRecipient_UserIdAndReadFalse(UUID userId);
}
