package com.nexushr.performance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "performance_goals")
public class PerformanceGoal {
    @Id
    private UUID id;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false)
    private int progress = 0;

    @Column(nullable = false, length = 30)
    private String status = "NOT_STARTED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PerformanceGoal() {}

    public PerformanceGoal(String email, String title, String description,
                           String category, LocalDate targetDate) {
        this.employeeEmail = email;
        this.title = title;
        this.description = description;
        this.category = category;
        this.targetDate = targetDate;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateProgress(int value, String newStatus) {
        this.progress = value;
        this.status = newStatus;
    }

    public UUID getId() { return id; }
    public String getEmployeeEmail() { return employeeEmail; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public LocalDate getTargetDate() { return targetDate; }
    public int getProgress() { return progress; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}