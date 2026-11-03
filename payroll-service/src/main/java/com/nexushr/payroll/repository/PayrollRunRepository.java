package com.nexushr.payroll.repository;

import com.nexushr.payroll.entity.PayrollRun;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    boolean existsByMonthAndYear(int month, int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from PayrollRun run where run.id = :id")
    Optional<PayrollRun> findByIdForUpdate(@Param("id") UUID id);
}
