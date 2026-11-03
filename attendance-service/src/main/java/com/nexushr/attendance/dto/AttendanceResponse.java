package com.nexushr.attendance.dto;

import com.nexushr.attendance.entity.AttendanceRecord;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
    UUID id,
    String employeeEmail,
    LocalDate attendanceDate,
    Instant checkIn,
    Instant checkOut,
    long workedMinutes,
    long overtimeMinutes,
    String status
) {
    public static AttendanceResponse from(
        AttendanceRecord record, int standardDailyMinutes
    ) {
        long worked = record.getCheckOut() == null
            ? 0
            : Duration.between(record.getCheckIn(), record.getCheckOut()).toMinutes();
        return new AttendanceResponse(
            record.getId(),
            record.getEmployeeEmail(),
            record.getAttendanceDate(),
            record.getCheckIn(),
            record.getCheckOut(),
            worked,
            Math.max(0, worked - standardDailyMinutes),
            record.getStatus()
        );
    }
}
