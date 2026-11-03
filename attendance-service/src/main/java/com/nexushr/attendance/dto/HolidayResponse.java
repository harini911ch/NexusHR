package com.nexushr.attendance.dto;

import com.nexushr.attendance.entity.Holiday;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayResponse(UUID id, String name, LocalDate date, String location) {
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
            holiday.getId(), holiday.getName(), holiday.getDate(), holiday.getLocation()
        );
    }
}
