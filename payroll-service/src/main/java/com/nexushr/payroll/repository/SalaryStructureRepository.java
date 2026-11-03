package com.nexushr.payroll.repository;

import com.nexushr.payroll.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {
    Optional<SalaryStructure> findByEmployeeId(UUID employeeId);
}