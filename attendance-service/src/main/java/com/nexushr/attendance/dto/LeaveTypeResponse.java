package com.nexushr.attendance.dto;

import com.nexushr.attendance.entity.LeaveType;
import java.util.UUID;

public record LeaveTypeResponse(
    UUID id,
    String code,
    String name,
    int annualAllowance,
    boolean paid,
    boolean active
) {
    public static LeaveTypeResponse from(LeaveType type) {
        return new LeaveTypeResponse(
            type.getId(), type.getCode(), type.getName(),
            type.getAnnualAllowance(), type.isPaid(), type.isActive()
        );
    }
}
