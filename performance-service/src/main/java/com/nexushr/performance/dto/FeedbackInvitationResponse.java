package com.nexushr.performance.dto;

import com.nexushr.performance.entity.PerformanceFeedbackInvitation;
import java.time.Instant;
import java.util.UUID;

public record FeedbackInvitationResponse(
    UUID id,
    UUID reviewId,
    String inviteeEmail,
    String relationship,
    String createdBy,
    Instant createdAt,
    Instant submittedAt
) {
    public static FeedbackInvitationResponse from(
        PerformanceFeedbackInvitation invitation
    ) {
        return new FeedbackInvitationResponse(
            invitation.getId(), invitation.getReviewId(), invitation.getInviteeEmail(),
            invitation.getRelationship(), invitation.getCreatedBy(),
            invitation.getCreatedAt(), invitation.getSubmittedAt()
        );
    }
}
