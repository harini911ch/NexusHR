package com.nexushr.auth.repository;

import com.nexushr.auth.entity.AuthAuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, UUID> {}
