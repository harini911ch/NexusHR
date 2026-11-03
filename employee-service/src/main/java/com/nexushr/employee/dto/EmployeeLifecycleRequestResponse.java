package com.nexushr.employee.dto;

import com.nexushr.employee.entity.EmployeeLifecycleRequest;
import java.time.Instant;
import java.util.UUID;

public record EmployeeLifecycleRequestResponse(
    UUID id,
    UUID employeeId,
    String action,
    String status,
    String requestedBy,
    String reason,
    String reviewedBy,
    String reviewNote,
    Instant requestedAt,
    Instant reviewedAt
) {
    public static EmployeeLifecycleRequestResponse from(
        EmployeeLifecycleRequest request
    ) {
        return new EmployeeLifecycleRequestResponse(
            request.getId(),
            request.getEmployeeId(),
            request.getAction().name(),
            request.getStatus().name(),
            request.getRequestedBy(),
            request.getReason(),
            request.getReviewedBy(),
            request.getReviewNote(),
            request.getRequestedAt(),
            request.getReviewedAt()
        );
    }
}
