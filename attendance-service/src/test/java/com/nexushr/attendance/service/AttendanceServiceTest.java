package com.nexushr.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.attendance.dto.LeaveRequestDto;
import com.nexushr.attendance.dto.LeaveReviewDto;
import com.nexushr.attendance.entity.LeaveBalance;
import com.nexushr.attendance.entity.LeaveRequest;
import com.nexushr.attendance.entity.LeaveType;
import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.attendance.repository.AttendanceRepository;
import com.nexushr.attendance.repository.HolidayRepository;
import com.nexushr.attendance.repository.LeaveBalanceRepository;
import com.nexushr.attendance.repository.LeaveRepository;
import com.nexushr.attendance.repository.LeaveTypeRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private LeaveRepository leaveRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private HolidayRepository holidayRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
            attendanceRepository, leaveRepository, leaveTypeRepository,
            leaveBalanceRepository, holidayRepository, eventPublisher,
            "UTC", LocalTime.of(9, 0), 480
        );
    }

    @Test
    void leaveRequestCountsWeekdaysAndChecksAnnualBalance() {
        LeaveType type = new LeaveType("CASUAL", "Casual leave", 10, true);
        LocalDate monday = nextMonday();
        when(leaveTypeRepository.findByCodeIgnoreCase("CASUAL")).thenReturn(Optional.of(type));
        when(leaveRepository
            .existsByEmployeeEmailAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq("person@example.com"), eq(List.of("PENDING", "APPROVED")),
                eq(monday.plusDays(4)), eq(monday)
            )).thenReturn(false);
        when(holidayRepository.findByDateBetweenOrderByDateAsc(monday, monday.plusDays(4)))
            .thenReturn(List.of());
        when(leaveBalanceRepository.findForUpdate(
            "person@example.com", type.getId(), monday.getYear()
        )).thenReturn(Optional.of(new LeaveBalance(
            "person@example.com", type.getId(), monday.getYear(), 10
        )));
        when(leaveRepository
            .findByEmployeeEmailIgnoreCaseAndStatusAndLeaveTypeIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                "person@example.com", "PENDING", "CASUAL",
                LocalDate.of(monday.getYear(), 12, 31),
                LocalDate.of(monday.getYear(), 1, 1)
            )).thenReturn(List.of());
        when(leaveRepository.save(any(LeaveRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyLeave(
            "Person@Example.com",
            new LeaveRequestDto("casual", monday, monday.plusDays(4), "Personal leave")
        );

        assertEquals(5, result.totalDays());
        assertEquals("PENDING", result.status());
        verify(leaveBalanceRepository).insertIfMissing(
            "person@example.com", type.getId(), monday.getYear(), 10
        );
    }

    @Test
    void checkInPublishesAnAfterCommitNotificationEvent() {
        when(attendanceRepository.findByEmployeeEmailAndAttendanceDate(
            eq("person@example.com"), any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(attendanceRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.checkIn("Person@Example.com");

        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void overlappingActiveLeaveIsRejectedBeforeBalanceOrSave() {
        LocalDate monday = nextMonday();
        when(leaveTypeRepository.findByCodeIgnoreCase("CASUAL"))
            .thenReturn(Optional.of(new LeaveType("CASUAL", "Casual leave", 10, true)));
        when(leaveRepository
            .existsByEmployeeEmailAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq("person@example.com"), eq(List.of("PENDING", "APPROVED")),
                eq(monday.plusDays(1)), eq(monday)
            )).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.applyLeave(
            "person@example.com",
            new LeaveRequestDto("CASUAL", monday, monday.plusDays(1), "Personal leave")
        ));

        verify(leaveBalanceRepository, never()).insertIfMissing(
            anyString(), any(UUID.class), anyInt(), anyInt()
        );
        verify(leaveRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void approvalUsesLeaveDaysAndRecordsReviewer() {
        LeaveType type = new LeaveType("CASUAL", "Casual leave", 10, true);
        LocalDate monday = nextMonday();
        LeaveRequest leave = new LeaveRequest(
            "person@example.com", "CASUAL", monday, monday.plusDays(1), 2, "Personal leave"
        );
        LeaveBalance balance = new LeaveBalance(
            "person@example.com", type.getId(), monday.getYear(), 10
        );
        when(leaveRepository.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveTypeRepository.findByCodeIgnoreCase("CASUAL")).thenReturn(Optional.of(type));
        when(holidayRepository.findByDateBetweenOrderByDateAsc(monday, monday.plusDays(1)))
            .thenReturn(List.of());
        when(leaveBalanceRepository.findForUpdate(
            "person@example.com", type.getId(), monday.getYear()
        )).thenReturn(Optional.of(balance));
        when(leaveRepository
            .findByEmployeeEmailIgnoreCaseAndStatusAndLeaveTypeIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                "person@example.com", "PENDING", "CASUAL",
                LocalDate.of(monday.getYear(), 12, 31),
                LocalDate.of(monday.getYear(), 1, 1)
            )).thenReturn(List.of());
        when(leaveRepository.save(leave)).thenReturn(leave);

        var result = service.reviewLeave(
            leave.getId(), new LeaveReviewDto("APPROVED", "Approved"), "HR@Example.com"
        );

        assertEquals("APPROVED", result.status());
        assertEquals("hr@example.com", result.reviewedBy());
        assertEquals(2, balance.getUsedDays());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    private static LocalDate nextMonday() {
        return LocalDate.now(ZoneId.of("UTC"))
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
