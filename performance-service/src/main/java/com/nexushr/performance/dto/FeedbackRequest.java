package com.nexushr.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(
    @Min(1) @Max(5) int rating,
    @NotBlank String comments
) {}