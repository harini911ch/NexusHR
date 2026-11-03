package com.nexushr.employee.dto;

import com.nexushr.employee.entity.Department;
import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
    UUID id,
    String name,
    String description,
    String managerEmail,
    UUID parentDepartmentId,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getDescription(),
            department.getManagerEmail(),
            department.getParentDepartmentId(),
            department.isActive(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
    }
}
