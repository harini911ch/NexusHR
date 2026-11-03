package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DesignationRequest(
    @NotBlank @Size(max = 120) String title,
    @NotNull UUID departmentId,
    @Size(max = 500) String description,
    Boolean active
) {}
