package com.nexushr.attendance.dto;

import java.util.UUID;

public record LeaveBalanceResponse(
    UUID leaveTypeId,
    String leaveType,
    int year,
    int grantedDays,
    int usedDays,
    int remainingDays
) {}
