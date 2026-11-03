package com.nexushr.employee.service;

import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.employee.dto.EmployeeLifecycleDecision;
import com.nexushr.employee.dto.EmployeeLifecycleRequestCreate;
import com.nexushr.employee.dto.EmployeeLifecycleRequestResponse;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.entity.EmployeeLifecycleAction;
import com.nexushr.employee.entity.EmployeeLifecycleRequest;
import com.nexushr.employee.entity.EmployeeLifecycleStatus;
import com.nexushr.employee.repository.EmployeeLifecycleRequestRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeLifecycleApprovalService {

    private static final Set<String> HR_ROLES =
        Set.of("SUPER_ADMIN", "HR_ADMIN", "HR_MANAGER");

    private final EmployeeRepository employeeRepository;
    private final EmployeeLifecycleRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeLifecycleApprovalService(
        EmployeeRepository employeeRepository,
        EmployeeLifecycleRequestRepository requestRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.employeeRepository = employeeRepository;
        this.requestRepository = requestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EmployeeLifecycleRequestResponse requestOwnOffboarding(
        String actorEmail, String reason
    ) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(
            normalizeEmail(actorEmail)
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Employee profile not found"
        ));
        return create(employee, EmployeeLifecycleAction.OFFBOARDING, actorEmail, reason);
    }

    @Transactional
    public EmployeeLifecycleRequestResponse requestForEmployee(
        UUID employeeId,
        String actorEmail,
        String actorRole,
        EmployeeLifecycleRequestCreate request
    ) {
        requireHr(actorRole);
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));
        return create(
            employee,
            parseAction(request.action()),
            actorEmail,
            request.reason()
        );
    }

    @Transactional(readOnly = true)
    public List<EmployeeLifecycleRequestResponse> listOwn(String actorEmail) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(
            normalizeEmail(actorEmail)
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Employee profile not found"
        ));
        return requestRepository.findByEmployeeIdOrderByRequestedAtDesc(employee.getId())
            .stream()
            .map(EmployeeLifecycleRequestResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeLifecycleRequestResponse> listPending(String actorRole) {
        requireHr(actorRole);
        return requestRepository
            .findByStatusOrderByRequestedAtAsc(EmployeeLifecycleStatus.PENDING)
            .stream()
            .map(EmployeeLifecycleRequestResponse::from)
            .toList();
    }

    @Transactional
    public EmployeeLifecycleRequestResponse decide(
        UUID requestId,
        String reviewerEmail,
        String reviewerRole,
        EmployeeLifecycleDecision decision
    ) {
        requireHr(reviewerRole);
        EmployeeLifecycleRequest request = requestRepository
            .findByIdForUpdate(requestId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Lifecycle request not found"
            ));
        if (request.getStatus() != EmployeeLifecycleStatus.PENDING) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Lifecycle request has already been reviewed"
            );
        }

        String reviewer = normalizeEmail(reviewerEmail);
        if (reviewer.equals(normalizeEmail(request.getRequestedBy()))) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "The requester cannot approve their own request"
            );
        }
        String note = decision.reviewNote() == null
            ? null
            : decision.reviewNote().trim();
        if (!decision.approved() && (note == null || note.isBlank())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A reason is required when rejecting a request"
            );
        }

        Employee employee = employeeRepository.findByIdForUpdate(
            request.getEmployeeId()
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Employee not found"
        ));
        if (decision.approved()) {
            applyApprovedTransition(employee, request.getAction());
            employeeRepository.save(employee);
        }

        request.decide(
            decision.approved()
                ? EmployeeLifecycleStatus.APPROVED
                : EmployeeLifecycleStatus.REJECTED,
            reviewer,
            note,
            Instant.now()
        );
        EmployeeLifecycleRequest saved = requestRepository.save(request);
        eventPublisher.publishEvent(new NotificationEvent(
            employee.getEmail(),
            "Lifecycle request " + saved.getStatus().toString().toLowerCase(Locale.ROOT),
            "Your " + actionMessage(saved.getAction()).toLowerCase(Locale.ROOT)
                + " was " + saved.getStatus().toString().toLowerCase(Locale.ROOT)
                + (note == null || note.isBlank() ? "." : ": " + note),
            "EMPLOYEE_LIFECYCLE",
            "EMPLOYEE_LIFECYCLE_REQUEST",
            saved.getId().toString()
        ));
        return EmployeeLifecycleRequestResponse.from(saved);
    }

    private EmployeeLifecycleRequestResponse create(
        Employee employee,
        EmployeeLifecycleAction action,
        String requestedBy,
        String reason
    ) {
        validateRequestState(employee, action);
        if (requestRepository.existsByEmployeeIdAndActionAndStatus(
            employee.getId(), action, EmployeeLifecycleStatus.PENDING
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A matching lifecycle request is already pending"
            );
        }
        EmployeeLifecycleRequest request = requestRepository.save(
            new EmployeeLifecycleRequest(
                employee.getId(), action, normalizeEmail(requestedBy), reason.trim()
            )
        );
        eventPublisher.publishEvent(new NotificationEvent(
            employee.getEmail(),
            "Lifecycle request submitted",
            actionMessage(action) + " is pending HR approval.",
            "EMPLOYEE_LIFECYCLE",
            "EMPLOYEE_LIFECYCLE_REQUEST",
            request.getId().toString()
        ));
        return EmployeeLifecycleRequestResponse.from(request);
    }

    private static void validateRequestState(
        Employee employee, EmployeeLifecycleAction action
    ) {
        boolean validState = switch (action) {
            case ONBOARDING -> "ONBOARDING".equals(employee.getEmploymentStatus());
            case OFFBOARDING -> Set.of("ACTIVE", "ON_LEAVE")
                .contains(employee.getEmploymentStatus());
        };
        if (!validState) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "The employee status does not allow this lifecycle request"
            );
        }
    }

    private static void applyApprovedTransition(
        Employee employee, EmployeeLifecycleAction action
    ) {
        validateRequestState(employee, action);
        employee.setEmploymentStatus(
            action == EmployeeLifecycleAction.ONBOARDING ? "ACTIVE" : "TERMINATED"
        );
    }

    private static EmployeeLifecycleAction parseAction(String action) {
        try {
            return EmployeeLifecycleAction.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unsupported lifecycle action", exception
            );
        }
    }

    private static String actionMessage(EmployeeLifecycleAction action) {
        return action == EmployeeLifecycleAction.ONBOARDING
            ? "Onboarding" : "Offboarding";
    }

    private static void requireHr(String role) {
        if (!HR_ROLES.contains(role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only HR administrators can manage lifecycle requests"
            );
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
