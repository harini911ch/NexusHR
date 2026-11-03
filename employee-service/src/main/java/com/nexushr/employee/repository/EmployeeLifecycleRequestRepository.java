package com.nexushr.employee.repository;

import com.nexushr.employee.entity.EmployeeLifecycleAction;
import com.nexushr.employee.entity.EmployeeLifecycleRequest;
import com.nexushr.employee.entity.EmployeeLifecycleStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeLifecycleRequestRepository
    extends JpaRepository<EmployeeLifecycleRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from EmployeeLifecycleRequest request " +
           "where request.id = :id")
    Optional<EmployeeLifecycleRequest> findByIdForUpdate(@Param("id") UUID id);

    List<EmployeeLifecycleRequest> findByEmployeeIdOrderByRequestedAtDesc(
        UUID employeeId
    );

    List<EmployeeLifecycleRequest> findByStatusOrderByRequestedAtAsc(
        EmployeeLifecycleStatus status
    );

    boolean existsByEmployeeIdAndActionAndStatus(
        UUID employeeId,
        EmployeeLifecycleAction action,
        EmployeeLifecycleStatus status
    );
}
