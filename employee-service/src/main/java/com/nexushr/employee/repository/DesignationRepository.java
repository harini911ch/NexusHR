package com.nexushr.employee.repository;

import com.nexushr.employee.entity.Designation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {
    Optional<Designation> findByTitleIgnoreCaseAndDepartmentId(
        String title, UUID departmentId
    );
    List<Designation> findByDepartmentIdAndActiveTrueOrderByTitleAsc(UUID departmentId);
}
