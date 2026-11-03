package com.nexushr.employee.dto;

import com.nexushr.employee.entity.Designation;
import java.time.Instant;
import java.util.UUID;

public record DesignationResponse(
    UUID id,
    String title,
    UUID departmentId,
    String description,
    boolean active,
    Instant createdAt
) {
    public static DesignationResponse from(Designation designation) {
        return new DesignationResponse(
            designation.getId(),
            designation.getTitle(),
            designation.getDepartmentId(),
            designation.getDescription(),
            designation.isActive(),
            designation.getCreatedAt()
        );
    }
}
