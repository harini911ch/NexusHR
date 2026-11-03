package com.nexushr.auth.service;

import com.nexushr.auth.entity.AuthAuditEvent;
import com.nexushr.auth.repository.AuthAuditEventRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditService {

    private final AuthAuditEventRepository repository;

    public AuthAuditService(AuthAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String eventType, String actorEmail, String subjectEmail) {
        repository.save(new AuthAuditEvent(
            eventType,
            normalizeEmail(actorEmail),
            normalizeEmail(subjectEmail)
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String email) {
        record("LOGIN_FAILED", email, email);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
