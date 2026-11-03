package com.nexushr.performance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "performance_reviews",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_review_employee_period",
           columnNames = {"employee_email", "review_period"}))
public class PerformanceReview {
    @Id
    private UUID id;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "reviewer_email", nullable = false)
    private String reviewerEmail;

    @Column(name = "review_period", nullable = false, length = 100)
    private String reviewPeriod;

    @Column(name = "self_rating")
    private Integer selfRating;

    @Column(name = "self_assessment", columnDefinition = "text")
    private String selfAssessment;

    @Column(name = "manager_rating")
    private Integer managerRating;

    @Column(columnDefinition = "text")
    private String strengths;

    @Column(name = "improvement_areas", columnDefinition = "text")
    private String improvementAreas;

    @Column(name = "manager_comments", columnDefinition = "text")
    private String managerComments;

    @Column(nullable = false, length = 30)
    private String status = "CREATED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "self_submitted_at")
    private Instant selfSubmittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected PerformanceReview() {}

    public PerformanceReview(String employeeEmail, String reviewerEmail, String reviewPeriod) {
        this.employeeEmail = employeeEmail;
        this.reviewerEmail = reviewerEmail;
        this.reviewPeriod = reviewPeriod;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }

    public void submitSelfAssessment(int rating, String assessment) {
        if ("COMPLETED".equals(status)) {
            throw new IllegalStateException("This review is already completed");
        }
        selfRating = rating;
        selfAssessment = assessment;
        selfSubmittedAt = Instant.now();
        status = "SELF_SUBMITTED";
    }

    public void submitManagerAssessment(int rating, String strengths,
                                        String improvements, String comments) {
        managerRating = rating;
        this.strengths = strengths;
        this.improvementAreas = improvements;
        managerComments = comments;
        reviewedAt = Instant.now();
        status = "COMPLETED";
    }

    public UUID getId() { return id; }
    public String getEmployeeEmail() { return employeeEmail; }
    public String getReviewerEmail() { return reviewerEmail; }
    public String getReviewPeriod() { return reviewPeriod; }
    public Integer getSelfRating() { return selfRating; }
    public String getSelfAssessment() { return selfAssessment; }
    public Integer getManagerRating() { return managerRating; }
    public String getStrengths() { return strengths; }
    public String getImprovementAreas() { return improvementAreas; }
    public String getManagerComments() { return managerComments; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSelfSubmittedAt() { return selfSubmittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}