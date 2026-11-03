package com.nexushr.payroll.dto;

import com.nexushr.payroll.entity.PayrollRun;
import java.time.Instant;
import java.util.UUID;

public record PayrollRunResponse(
    UUID id,
    int month,
    int year,
    String status,
    String createdBy,
    String approvedBy,
    Instant createdAt,
    Instant approvedAt,
    int payslipCount
) {
    public static PayrollRunResponse from(PayrollRun run, int payslipCount) {
        return new PayrollRunResponse(
            run.getId(), run.getMonth(), run.getYear(), run.getStatus(),
            run.getCreatedBy(), run.getApprovedBy(), run.getCreatedAt(),
            run.getApprovedAt(), payslipCount
        );
    }
}
