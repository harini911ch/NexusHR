package com.nexushr.attendance.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record LeaveRequestDto(
    @NotBlank String leaveType,
    @NotNull @FutureOrPresent LocalDate startDate,
    @NotNull @FutureOrPresent LocalDate endDate,
    @NotBlank @Size(max = 1000) String reason
) {}