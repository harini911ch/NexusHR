package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeLifecycleDecision(
    @NotNull Boolean approved,
    @Size(max = 1000) String reviewNote
) {}
