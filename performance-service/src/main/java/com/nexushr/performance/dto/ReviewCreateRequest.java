package com.nexushr.performance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReviewCreateRequest(
    @NotBlank @Email String employeeEmail,
    @NotBlank String reviewPeriod
) {}