package com.nexushr.payroll.repository;

import com.nexushr.payroll.entity.PayrollRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, UUID> {
    List<PayrollRecord> findByEmployeeEmailIgnoreCaseOrderByPayrollYearDescPayrollMonthDesc(String email);
    Optional<PayrollRecord> findByEmployeeIdAndPayrollMonthAndPayrollYear(
        UUID employeeId, Integer month, Integer year);
    List<PayrollRecord> findByPayrollRunIdOrderByEmployeeCodeAsc(UUID payrollRunId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from PayrollRecord record where record.id = :id")
    Optional<PayrollRecord> findByIdForUpdate(@Param("id") UUID id);
}