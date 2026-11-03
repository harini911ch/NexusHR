package com.nexushr.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GoalProgressRequest(
    @Min(0) @Max(100) int progress,
    @NotBlank String status
) {}