package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeOffboardingRequest(
    @NotBlank @Size(max = 1000) String reason
) {}
