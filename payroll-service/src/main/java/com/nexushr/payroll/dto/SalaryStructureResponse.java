package com.nexushr.payroll.dto;

import com.nexushr.payroll.entity.SalaryStructure;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalaryStructureResponse(
    UUID id,
    UUID employeeId,
    String employeeCode,
    String employeeEmail,
    BigDecimal basicSalary,
    BigDecimal housingAllowance,
    BigDecimal transportAllowance,
    BigDecimal otherAllowance,
    BigDecimal taxPercentage,
    BigDecimal otherDeduction,
    String currency,
    LocalDate effectiveFrom
) {
    public static SalaryStructureResponse from(SalaryStructure salary) {
        return new SalaryStructureResponse(
            salary.getId(), salary.getEmployeeId(), salary.getEmployeeCode(),
            salary.getEmployeeEmail(), salary.getBasicSalary(),
            salary.getHousingAllowance(), salary.getTransportAllowance(),
            salary.getOtherAllowance(), salary.getTaxPercentage(),
            salary.getOtherDeduction(), salary.getCurrency(), salary.getEffectiveFrom()
        );
    }
}
