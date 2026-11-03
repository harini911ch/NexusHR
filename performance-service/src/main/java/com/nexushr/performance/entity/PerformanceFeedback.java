package com.nexushr.performance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "performance_feedback",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_feedback_reviewer",
           columnNames = {"review_id", "from_email"}))
public class PerformanceFeedback {
    @Id
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(nullable = false, length = 50)
    private String relationship;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comments;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PerformanceFeedback() {}

    public PerformanceFeedback(UUID reviewId, String fromEmail,
                               String relationship, int rating, String comments) {
        this.reviewId = reviewId;
        this.fromEmail = fromEmail;
        this.relationship = relationship;
        this.rating = rating;
        this.comments = comments;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getReviewId() { return reviewId; }
    public String getFromEmail() { return fromEmail; }
    public String getRelationship() { return relationship; }
    public int getRating() { return rating; }
    public String getComments() { return comments; }
    public Instant getCreatedAt() { return createdAt; }
}