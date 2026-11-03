package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.LeaveRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveRepository extends JpaRepository<LeaveRequest, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select leave from LeaveRequest leave where leave.id = :id")
    java.util.Optional<LeaveRequest> findByIdForUpdate(@Param("id") UUID id);
    List<LeaveRequest> findByEmployeeEmailOrderByCreatedAtDesc(String email);
    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByEmployeeEmailAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        String email, List<String> statuses, LocalDate end, LocalDate start
    );
    List<LeaveRequest> findByEmployeeEmailIgnoreCaseAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        String email, String status, LocalDate end, LocalDate start
    );
    List<LeaveRequest> findByEmployeeEmailIgnoreCaseAndStatusAndLeaveTypeIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        String email, String status, String leaveType, LocalDate end, LocalDate start
    );
    List<LeaveRequest> findByEmployeeEmailAndStatusAndStartDateBetween(
        String email, String status, LocalDate start, LocalDate end
    );
}