package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PerformanceFeedbackInvitation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerformanceFeedbackInvitationRepository
    extends JpaRepository<PerformanceFeedbackInvitation, UUID> {

    boolean existsByReviewIdAndInviteeEmailIgnoreCase(UUID reviewId, String inviteeEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select invitation from PerformanceFeedbackInvitation invitation
        where invitation.reviewId = :reviewId
          and lower(invitation.inviteeEmail) = lower(:email)
        """)
    Optional<PerformanceFeedbackInvitation> findForUpdate(
        @Param("reviewId") UUID reviewId, @Param("email") String email
    );

    List<PerformanceFeedbackInvitation>
        findByInviteeEmailIgnoreCaseAndSubmittedAtIsNullOrderByCreatedAtDesc(String email);
}
