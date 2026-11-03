package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.AttendanceRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findByEmployeeEmailAndAttendanceDate(
        String email, LocalDate date
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select record from AttendanceRecord record
        where lower(record.employeeEmail) = lower(:email)
          and record.attendanceDate = :date
        """)
    Optional<AttendanceRecord> findForUpdate(
        @Param("email") String email, @Param("date") LocalDate date
    );
    List<AttendanceRecord> findByEmployeeEmailOrderByAttendanceDateDesc(String email);
}