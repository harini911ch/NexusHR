package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.LeaveType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    Optional<LeaveType> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    List<LeaveType> findByActiveTrueOrderByNameAsc();
}
