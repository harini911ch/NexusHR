package com.nexushr.insights.dto;

import jakarta.validation.constraints.*;

public record EngagementRequest(
    @NotBlank @Email String employeeEmail,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double attendanceRate,
    @NotNull @DecimalMin("1.0") @DecimalMax("5.0") Double feedbackRating,
    @NotNull @DecimalMin("1.0") @DecimalMax("5.0") Double managerRating,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double goalProgress
) {}