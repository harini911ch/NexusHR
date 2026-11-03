package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DepartmentRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Size(max = 180) String managerEmail,
    UUID parentDepartmentId,
    Boolean active
) {}
