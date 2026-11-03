package com.nexushr.insights.dto;

import jakarta.validation.constraints.*;

public record AttritionRequest(
    @NotBlank @Email String employeeEmail,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double attendanceRate,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double engagementScore,
    @NotNull @DecimalMin("0.0") @DecimalMax("5.0") Double performanceRating,
    @NotNull @Min(0) Integer tenureMonths,
    @NotNull @Min(0) Integer recentAbsences,
    @NotNull @Min(0) Integer monthsSinceLastPromotion
) {}