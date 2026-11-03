package com.nexushr.attendance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveTypeRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 80) String name,
    @Min(0) int annualAllowance,
    boolean paid,
    Boolean active
) {}
