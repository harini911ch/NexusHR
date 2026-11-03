package com.nexushr.attendance.dto;

import com.nexushr.attendance.entity.LeaveRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
    UUID id,
    String employeeEmail,
    String leaveType,
    LocalDate startDate,
    LocalDate endDate,
    int totalDays,
    String reason,
    String status,
    String reviewedBy,
    String reviewComment,
    Instant createdAt,
    Instant reviewedAt
) {
    public static LeaveRequestResponse from(LeaveRequest request) {
        return new LeaveRequestResponse(
            request.getId(),
            request.getEmployeeEmail(),
            request.getLeaveType(),
            request.getStartDate(),
            request.getEndDate(),
            request.getTotalDays(),
            request.getReason(),
            request.getStatus(),
            request.getReviewedBy(),
            request.getReviewComment(),
            request.getCreatedAt(),
            request.getReviewedAt()
        );
    }
}
