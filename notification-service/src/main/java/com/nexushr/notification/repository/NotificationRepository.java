package com.nexushr.notification.repository;

import com.nexushr.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email);

    long countByRecipientEmailAndReadFalse(String email);

    Optional<Notification> findByIdAndRecipientEmail(UUID id, String email);

    @Modifying
    @Query(value = """
        INSERT INTO notifications (
            id, source_event_id, recipient_email, title, message, type,
            related_entity_type, related_entity_id, is_read, created_at
        ) VALUES (
            :id, :sourceEventId, :recipientEmail, :title, :message, :type,
            :relatedEntityType, :relatedEntityId, FALSE, CURRENT_TIMESTAMP
        )
        ON CONFLICT (source_event_id) DO NOTHING
        """, nativeQuery = true)
    int insertInternalIfAbsent(
        @Param("id") UUID id,
        @Param("sourceEventId") UUID sourceEventId,
        @Param("recipientEmail") String recipientEmail,
        @Param("title") String title,
        @Param("message") String message,
        @Param("type") String type,
        @Param("relatedEntityType") String relatedEntityType,
        @Param("relatedEntityId") String relatedEntityId
    );

    Optional<Notification> findBySourceEventId(UUID sourceEventId);
}
