package com.nexushr.attendance.service;

import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.attendance.dto.*;
import com.nexushr.attendance.entity.*;
import com.nexushr.attendance.repository.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {

    private static final List<String> ACTIVE_LEAVE_STATUSES = List.of("PENDING", "APPROVED");

    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final HolidayRepository holidayRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ZoneId attendanceZone;
    private final LocalTime workdayStart;
    private final int standardDailyMinutes;

    public AttendanceService(
        AttendanceRepository attendanceRepository,
        LeaveRepository leaveRepository,
        LeaveTypeRepository leaveTypeRepository,
        LeaveBalanceRepository leaveBalanceRepository,
        HolidayRepository holidayRepository,
        ApplicationEventPublisher eventPublisher,
        @Value("${app.attendance.zone:UTC}") String attendanceZone,
        @Value("${app.attendance.workday-start:09:00}") LocalTime workdayStart,
        @Value("${app.attendance.standard-daily-minutes:480}") int standardDailyMinutes
    ) {
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.holidayRepository = holidayRepository;
        this.eventPublisher = eventPublisher;
        this.attendanceZone = ZoneId.of(attendanceZone);
        this.workdayStart = workdayStart;
        this.standardDailyMinutes = standardDailyMinutes;
    }

    @Transactional
    public AttendanceResponse checkIn(String email) {
        Instant now = Instant.now();
        ZonedDateTime localTime = now.atZone(attendanceZone);
        LocalDate today = localTime.toLocalDate();

        String normalizedEmail = normalizeEmail(email);
        if (attendanceRepository.findByEmployeeEmailAndAttendanceDate(
            normalizedEmail, today
        ).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Attendance already marked for today"
            );
        }

        AttendanceRecord record = new AttendanceRecord(
            normalizedEmail, now, today, localTime.toLocalTime().isAfter(workdayStart)
        );
        AttendanceRecord saved = attendanceRepository.save(record);
        eventPublisher.publishEvent(new NotificationEvent(
            normalizedEmail,
            "Attendance checked in",
            "Your check-in for " + today + " was recorded.",
            "ATTENDANCE",
            "ATTENDANCE",
            saved.getId().toString()
        ));
        return attendanceResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(String email) {
        LocalDate today = LocalDate.now(attendanceZone);
        AttendanceRecord record = attendanceRepository.findForUpdate(normalizeEmail(email), today)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Please check in before checking out"
            ));

        try {
            record.checkOut(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        AttendanceRecord saved = attendanceRepository.save(record);
        eventPublisher.publishEvent(new NotificationEvent(
            saved.getEmployeeEmail(),
            "Attendance checked out",
            "Your check-out for " + today + " was recorded.",
            "ATTENDANCE",
            "ATTENDANCE",
            saved.getId().toString()
        ));
        return attendanceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> myAttendance(String email) {
        return attendanceRepository.findByEmployeeEmailOrderByAttendanceDateDesc(
            normalizeEmail(email)
        )
            .stream().map(this::attendanceResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse applyLeave(String email, LeaveRequestDto request) {
        validateLeaveDates(request.startDate(), request.endDate());

        LeaveType type = leaveTypeRepository.findByCodeIgnoreCase(
            normalizeCode(request.leaveType())
        )
            .filter(LeaveType::isActive)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "The selected leave type is unavailable"
            ));

        if (leaveRepository.existsByEmployeeEmailAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            normalizeEmail(email), ACTIVE_LEAVE_STATUSES,
            request.endDate(), request.startDate()
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "You already have a pending or approved leave in this date range"
            );
        }

        Map<Integer, Integer> requestedDays = workingDaysByYear(
            request.startDate(), request.endDate()
        );
        if (requestedDays.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave must include at least one working day"
            );
        }
        if (type.isPaid()) {
            verifyBalance(email, type, requestedDays, null);
        }

        int totalDays = requestedDays.values().stream().mapToInt(Integer::intValue).sum();
        LeaveRequest leave = leaveRepository.save(new LeaveRequest(
            normalizeEmail(email),
            type.getCode(),
            request.startDate(),
            request.endDate(),
            totalDays,
            request.reason().trim()
        ));
        return LeaveRequestResponse.from(leave);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> myLeaves(String email) {
        return leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(normalizeEmail(email))
            .stream().map(LeaveRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> pendingLeaves() {
        return leaveRepository.findByStatusOrderByCreatedAtAsc("PENDING")
            .stream().map(LeaveRequestResponse::from).toList();
    }

    @Transactional
    public LeaveRequestResponse reviewLeave(
        UUID id, LeaveReviewDto request, String reviewer
    ) {
        LeaveRequest leave = leaveRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave request not found"
            ));
        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED"
            );
        }

        LeaveType type = leaveTypeRepository.findByCodeIgnoreCase(leave.getLeaveType())
            .orElseThrow(() -> new IllegalStateException(
                "Leave request references an unknown leave type"
            ));
        Map<Integer, Integer> daysByYear = workingDaysByYear(
            leave.getStartDate(), leave.getEndDate()
        );
        if ("APPROVED".equals(decision) && type.isPaid()) {
            verifyBalance(leave.getEmployeeEmail(), type, daysByYear, leave.getId());
            for (var entry : new TreeMap<>(daysByYear).entrySet()) {
                LeaveBalance balance = lockBalance(
                    leave.getEmployeeEmail(), type, entry.getKey()
                );
                balance.addUsedDays(entry.getValue());
                leaveBalanceRepository.save(balance);
            }
        }

        try {
            leave.review(decision, normalizeEmail(reviewer), request.comment());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        LeaveRequest saved = leaveRepository.save(leave);
        eventPublisher.publishEvent(new NotificationEvent(
            saved.getEmployeeEmail(),
            "Leave request " + decision.toLowerCase(Locale.ROOT),
            "Your " + saved.getLeaveType() + " leave request from "
                + saved.getStartDate() + " to " + saved.getEndDate()
                + " was " + decision.toLowerCase(Locale.ROOT) + ".",
            "LEAVE",
            "LEAVE_REQUEST",
            saved.getId().toString()
        ));
        return LeaveRequestResponse.from(saved);
    }

    @Transactional
    public List<LeaveBalanceResponse> leaveBalance(String email, int year) {
        if (year < 2000 || year > 2200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid balance year");
        }
        String normalizedEmail = normalizeEmail(email);
        return leaveTypeRepository.findAll().stream()
            .filter(LeaveType::isActive)
            .sorted(Comparator.comparing(LeaveType::getName))
            .map(type -> {
                LeaveBalance balance = lockBalance(normalizedEmail, type, year);
                return new LeaveBalanceResponse(
                    type.getId(), type.getCode(), year,
                    balance.getGrantedDays(), balance.getUsedDays(),
                    balance.getRemainingDays()
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> leaveTypes(boolean includeInactive) {
        return leaveTypeRepository.findAll().stream()
            .filter(type -> includeInactive || type.isActive())
            .sorted(Comparator.comparing(LeaveType::getName))
            .map(LeaveTypeResponse::from)
            .toList();
    }

    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        String code = normalizeCode(request.code());
        if (leaveTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Leave type code already exists"
            );
        }
        LeaveType type = new LeaveType(
            code, request.name().trim(), request.annualAllowance(), request.paid()
        );
        type.update(code, request.name().trim(), request.annualAllowance(),
            request.paid(), request.active() == null || request.active());
        return LeaveTypeResponse.from(leaveTypeRepository.save(type));
    }

    @Transactional
    public LeaveTypeResponse updateLeaveType(UUID id, LeaveTypeRequest request) {
        LeaveType type = leaveTypeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave type not found"
            ));
        String code = normalizeCode(request.code());
        if (leaveTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Leave type code already exists"
            );
        }
        int updatedBalances = leaveBalanceRepository.updateGrant(
            type.getId(), request.annualAllowance()
        );
        long existingBalances = leaveBalanceRepository.countByLeaveTypeId(type.getId());
        if (updatedBalances != existingBalances) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Allowance cannot be less than leave already used"
            );
        }
        type.update(code, request.name().trim(), request.annualAllowance(),
            request.paid(), request.active() == null || type.isActive());
        return LeaveTypeResponse.from(leaveTypeRepository.save(type));
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> holidays(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 370) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Holiday date range must be ordered and no longer than 371 days"
            );
        }
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to)
            .stream().map(HolidayResponse::from).toList();
    }

    @Transactional
    public HolidayResponse addHoliday(HolidayRequest request) {
        if (holidayRepository.existsByDateAndLocation(
            request.date(), trimToNull(request.location()))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A holiday already exists for this date and location"
            );
        }
        return HolidayResponse.from(holidayRepository.save(new Holiday(
            request.name().trim(), request.date(), trimToNull(request.location())
        )));
    }

    private void verifyBalance(
        String email, LeaveType type, Map<Integer, Integer> requestedDays, UUID currentRequestId
    ) {
        for (var entry : new TreeMap<>(requestedDays).entrySet()) {
            LeaveBalance balance = lockBalance(email, type, entry.getKey());
            int pendingDays = pendingRequests(email, type, entry.getKey()).stream()
                .filter(request -> !request.getId().equals(currentRequestId))
                .mapToInt(request -> workingDaysInYear(
                    request.getStartDate(), request.getEndDate(), entry.getKey()
                ))
                .sum();
            if (balance.getUsedDays() + pendingDays + entry.getValue()
                > balance.getGrantedDays()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient " + type.getName() + " balance for " + entry.getKey()
                );
            }
        }
    }

    private List<LeaveRequest> pendingRequests(
        String email, LeaveType type, int year
    ) {
        return leaveRepository
            .findByEmployeeEmailIgnoreCaseAndStatusAndLeaveTypeIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                normalizeEmail(email),
                "PENDING",
                type.getCode(),
                LocalDate.of(year, 12, 31),
                LocalDate.of(year, 1, 1)
            );
    }

    private LeaveBalance lockBalance(String email, LeaveType type, int year) {
        String normalizedEmail = normalizeEmail(email);
        leaveBalanceRepository.insertIfMissing(
            normalizedEmail, type.getId(), year, type.getAnnualAllowance()
        );
        return leaveBalanceRepository.findForUpdate(normalizedEmail, type.getId(), year)
            .orElseThrow(() -> new IllegalStateException(
                "Unable to initialize the employee leave balance"
            ));
    }

    private Map<Integer, Integer> workingDaysByYear(LocalDate start, LocalDate end) {
        Map<Integer, Integer> result = new TreeMap<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            int days = workingDaysInYear(start, end, year);
            if (days > 0) result.put(year, days);
        }
        return result;
    }

    private int workingDaysInYear(LocalDate start, LocalDate end, int year) {
        LocalDate first = start.isAfter(LocalDate.of(year, 1, 1))
            ? start : LocalDate.of(year, 1, 1);
        LocalDate last = end.isBefore(LocalDate.of(year, 12, 31))
            ? end : LocalDate.of(year, 12, 31);
        if (first.isAfter(last)) return 0;
        Set<LocalDate> holidays = holidayRepository
            .findByDateBetweenOrderByDateAsc(first, last).stream()
            .map(Holiday::getDate)
            .collect(java.util.stream.Collectors.toSet());
        int days = 0;
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY &&
                date.getDayOfWeek() != DayOfWeek.SUNDAY &&
                !holidays.contains(date)) {
                days++;
            }
        }
        return days;
    }

    private void validateLeaveDates(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(attendanceZone);
        if (end.isBefore(start)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "End date must not be before start date"
            );
        }
        if (start.isBefore(today)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave cannot start in the past"
            );
        }
        if (ChronoUnit.DAYS.between(start, end) > 90) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A leave request cannot exceed 91 calendar days"
            );
        }
    }

    private AttendanceResponse attendanceResponse(AttendanceRecord record) {
        return AttendanceResponse.from(record, standardDailyMinutes);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
