package com.nexushr.performance.dto;

import com.nexushr.performance.entity.PerformanceFeedback;
import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
    UUID id,
    UUID reviewId,
    String fromEmail,
    String relationship,
    int rating,
    String comments,
    Instant createdAt
) {
    public static FeedbackResponse from(PerformanceFeedback feedback) {
        return new FeedbackResponse(
            feedback.getId(), feedback.getReviewId(), feedback.getFromEmail(),
            feedback.getRelationship(), feedback.getRating(), feedback.getComments(),
            feedback.getCreatedAt()
        );
    }
}
