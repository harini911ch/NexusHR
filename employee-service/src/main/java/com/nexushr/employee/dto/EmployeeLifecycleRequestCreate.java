package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeLifecycleRequestCreate(
    @NotBlank @Pattern(regexp = "ONBOARDING|OFFBOARDING") String action,
    @NotBlank @Size(max = 1000) String reason
) {}
