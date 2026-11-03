package com.nexushr.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveReviewDto(
    @NotBlank String decision,
    @Size(max = 1000) String comment
) {}