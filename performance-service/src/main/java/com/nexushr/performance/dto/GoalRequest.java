package com.nexushr.performance.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GoalRequest(
    @NotBlank String title,
    String description,
    @NotBlank String category,
    @NotNull @FutureOrPresent LocalDate targetDate
) {}