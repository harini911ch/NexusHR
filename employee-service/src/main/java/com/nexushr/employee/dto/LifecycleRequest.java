package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LifecycleRequest(
    @NotBlank @Pattern(regexp = "ONBOARDING|ACTIVE|ON_LEAVE|TERMINATED")
    String status
) {}
