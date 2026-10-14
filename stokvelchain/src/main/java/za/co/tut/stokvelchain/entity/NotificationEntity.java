package za.co.tut.stokvelchain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import za.co.tut.stokvelchain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Id of the record this notification is about (e.g. a loanId), mirroring TransactionEntity's sourceRecordId. */
    @Column(name = "related_record_id")
    private UUID relatedRecordId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_recipient"))
    private UserEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id",
            foreignKey = @ForeignKey(name = "fk_notification_group"))
    private StokvelGroupEntity group;
}
