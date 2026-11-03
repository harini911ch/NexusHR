package com.nexushr.employee.repository;

import com.nexushr.employee.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Department> findByActiveTrueOrderByNameAsc();
    List<Department> findByManagerEmailIgnoreCaseAndActiveTrue(String managerEmail);
}
