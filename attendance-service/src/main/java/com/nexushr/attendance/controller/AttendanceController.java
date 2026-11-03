package com.nexushr.attendance.controller;

import com.nexushr.attendance.dto.*;
import com.nexushr.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/attendance/check-in")
    public AttendanceResponse checkIn(Authentication authentication) {
        return service.checkIn(authentication.getName());
    }

    @PostMapping("/attendance/check-out")
    public AttendanceResponse checkOut(Authentication authentication) {
        return service.checkOut(authentication.getName());
    }

    @PostMapping("/attendance/biometric/simulate")
    public Map<String, Object> simulateBiometric(
        Authentication authentication,
        @RequestParam String event
    ) {
        AttendanceResponse record = switch (event.trim().toUpperCase()) {
            case "CHECK_IN" -> service.checkIn(authentication.getName());
            case "CHECK_OUT" -> service.checkOut(authentication.getName());
            default -> throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Event must be CHECK_IN or CHECK_OUT"
            );
        };
        return Map.of(
            "integrationMode", "SIMULATION",
            "disclaimer", "This event is simulated and is not connected to biometric hardware.",
            "attendance", record
        );
    }

    @GetMapping("/attendance/me")
    public List<AttendanceResponse> myAttendance(Authentication authentication) {
        return service.myAttendance(authentication.getName());
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestResponse applyLeave(
        Authentication authentication,
        @Valid @RequestBody LeaveRequestDto request
    ) {
        return service.applyLeave(authentication.getName(), request);
    }

    @GetMapping("/leaves/me")
    public List<LeaveRequestResponse> myLeaves(Authentication authentication) {
        return service.myLeaves(authentication.getName());
    }

    @GetMapping("/leaves/balance")
    public List<LeaveBalanceResponse> leaveBalance(
        Authentication authentication,
        @RequestParam(required = false) Integer year
    ) {
        return service.leaveBalance(
            authentication.getName(),
            year == null ? java.time.Year.now().getValue() : year
        );
    }

    @GetMapping("/leaves/pending")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    public List<LeaveRequestResponse> pendingLeaves() {
        return service.pendingLeaves();
    }

    @PutMapping("/leaves/{id}/review")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    public LeaveRequestResponse reviewLeave(
        @PathVariable UUID id,
        @Valid @RequestBody LeaveReviewDto request,
        Authentication authentication
    ) {
        return service.reviewLeave(id, request, authentication.getName());
    }

    @GetMapping("/leave-types")
    public List<LeaveTypeResponse> leaveTypes(
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.leaveTypes(includeInactive);
    }

    @PostMapping("/leave-types")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveTypeResponse createLeaveType(
        @Valid @RequestBody LeaveTypeRequest request
    ) {
        return service.createLeaveType(request);
    }

    @PutMapping("/leave-types/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    public LeaveTypeResponse updateLeaveType(
        @PathVariable UUID id,
        @Valid @RequestBody LeaveTypeRequest request
    ) {
        return service.updateLeaveType(id, request);
    }

    @GetMapping("/holidays")
    public List<HolidayResponse> holidays(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.holidays(from, to);
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse addHoliday(@Valid @RequestBody HolidayRequest request) {
        return service.addHoliday(request);
    }
}
