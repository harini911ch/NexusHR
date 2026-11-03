package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    private UUID id;

    @Column(name = "employee_email", nullable = false, length = 180)
    private String employeeEmail;

    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected LeaveRequest() {}

    public LeaveRequest(
        String email, String type, LocalDate start, LocalDate end,
        int days, String reason
    ) {
        this.id = UUID.randomUUID();
        this.employeeEmail = email;
        this.leaveType = type;
        this.startDate = start;
        this.endDate = end;
        this.totalDays = days;
        this.reason = reason;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmployeeEmail() { return employeeEmail; }
    public String getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getTotalDays() { return totalDays; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewComment() { return reviewComment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }

    public void review(String decision, String reviewer, String comment) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Only pending requests can be reviewed");
        }
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        }
        this.status = decision;
        this.reviewedBy = reviewer;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
    }
}