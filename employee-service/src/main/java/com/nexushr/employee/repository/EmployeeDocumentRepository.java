package com.nexushr.employee.repository;

import com.nexushr.employee.entity.EmployeeDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeDocumentRepository
    extends JpaRepository<EmployeeDocument, UUID> {

    List<EmployeeDocument> findByEmployeeIdOrderByUploadedAtDesc(UUID employeeId);

    Optional<EmployeeDocument> findByIdAndEmployeeId(UUID id, UUID employeeId);
}
