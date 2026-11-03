package com.nexushr.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "performance_feedback_invitations", uniqueConstraints = @UniqueConstraint(
    name = "uk_feedback_invitation_review_invitee",
    columnNames = {"review_id", "invitee_email"}
))
public class PerformanceFeedbackInvitation {

    @Id
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "invitee_email", nullable = false, length = 180)
    private String inviteeEmail;

    @Column(nullable = false, length = 50)
    private String relationship;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected PerformanceFeedbackInvitation() {}

    public PerformanceFeedbackInvitation(
        UUID reviewId, String inviteeEmail, String relationship, String createdBy
    ) {
        this.reviewId = reviewId;
        this.inviteeEmail = inviteeEmail;
        this.relationship = relationship;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public void markSubmitted() {
        if (submittedAt != null) {
            throw new IllegalStateException("Feedback invitation has already been used");
        }
        submittedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getReviewId() { return reviewId; }
    public String getInviteeEmail() { return inviteeEmail; }
    public String getRelationship() { return relationship; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
}
