package com.nexushr.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ManagerAssessmentRequest(
    @Min(1) @Max(5) int rating,
    @NotBlank String strengths,
    @NotBlank String improvementAreas,
    @NotBlank String comments
) {}