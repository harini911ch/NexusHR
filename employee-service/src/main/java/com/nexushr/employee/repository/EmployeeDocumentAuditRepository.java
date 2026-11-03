package com.nexushr.employee.repository;

import com.nexushr.employee.entity.EmployeeDocumentAuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeDocumentAuditRepository
    extends JpaRepository<EmployeeDocumentAuditEvent, UUID> {}
