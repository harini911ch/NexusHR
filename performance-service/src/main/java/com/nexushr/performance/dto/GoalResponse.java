package com.nexushr.performance.dto;

import com.nexushr.performance.entity.PerformanceGoal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
    UUID id,
    String employeeEmail,
    String title,
    String description,
    String category,
    LocalDate targetDate,
    int progress,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static GoalResponse from(PerformanceGoal goal) {
        return new GoalResponse(
            goal.getId(), goal.getEmployeeEmail(), goal.getTitle(), goal.getDescription(),
            goal.getCategory(), goal.getTargetDate(), goal.getProgress(), goal.getStatus(),
            goal.getCreatedAt(), goal.getUpdatedAt()
        );
    }
}
