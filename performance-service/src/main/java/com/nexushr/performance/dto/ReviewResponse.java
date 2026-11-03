package com.nexushr.performance.dto;

import com.nexushr.performance.entity.PerformanceReview;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    String employeeEmail,
    String reviewerEmail,
    String reviewPeriod,
    Integer selfRating,
    String selfAssessment,
    Integer managerRating,
    String strengths,
    String improvementAreas,
    String managerComments,
    String status,
    Instant createdAt,
    Instant selfSubmittedAt,
    Instant reviewedAt
) {
    public static ReviewResponse from(PerformanceReview review) {
        return new ReviewResponse(
            review.getId(), review.getEmployeeEmail(), review.getReviewerEmail(),
            review.getReviewPeriod(), review.getSelfRating(), review.getSelfAssessment(),
            review.getManagerRating(), review.getStrengths(), review.getImprovementAreas(),
            review.getManagerComments(), review.getStatus(), review.getCreatedAt(),
            review.getSelfSubmittedAt(), review.getReviewedAt()
        );
    }
}
