package com.nexushr.payroll.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalaryStructureRequest(
    @NotNull UUID employeeId,
    @NotBlank String employeeCode,
    @NotBlank @Email String employeeEmail,
    @NotNull @DecimalMin("0.00") BigDecimal basicSalary,
    @NotNull @DecimalMin("0.00") BigDecimal housingAllowance,
    @NotNull @DecimalMin("0.00") BigDecimal transportAllowance,
    @NotNull @DecimalMin("0.00") BigDecimal otherAllowance,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxPercentage,
    @NotNull @DecimalMin("0.00") BigDecimal otherDeduction,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull LocalDate effectiveFrom
) {}