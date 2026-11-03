package com.nexushr.payroll.dto;

import com.nexushr.payroll.entity.PayrollRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayslipResponse(
    UUID id,
    UUID payrollRunId,
    UUID employeeId,
    String employeeCode,
    String employeeEmail,
    int month,
    int year,
    BigDecimal basicSalary,
    BigDecimal totalAllowances,
    BigDecimal grossSalary,
    BigDecimal taxDeduction,
    BigDecimal otherDeduction,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    String currency,
    String status,
    Instant processedAt
) {
    public static PayslipResponse from(PayrollRecord record) {
        return new PayslipResponse(
            record.getId(), record.getPayrollRunId(), record.getEmployeeId(),
            record.getEmployeeCode(), record.getEmployeeEmail(),
            record.getPayrollMonth(), record.getPayrollYear(),
            record.getBasicSalary(), record.getTotalAllowances(),
            record.getGrossSalary(), record.getTaxDeduction(),
            record.getOtherDeduction(), record.getTotalDeductions(),
            record.getNetSalary(), record.getCurrency(), record.getPaymentStatus(),
            record.getProcessedAt()
        );
    }
}
