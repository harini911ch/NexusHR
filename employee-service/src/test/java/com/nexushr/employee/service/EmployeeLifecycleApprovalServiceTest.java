package com.nexushr.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.employee.dto.EmployeeLifecycleDecision;
import com.nexushr.employee.dto.EmployeeLifecycleRequestCreate;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.entity.EmployeeLifecycleAction;
import com.nexushr.employee.entity.EmployeeLifecycleRequest;
import com.nexushr.employee.entity.EmployeeLifecycleStatus;
import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.employee.repository.EmployeeLifecycleRequestRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EmployeeLifecycleApprovalServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeLifecycleRequestRepository requestRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EmployeeLifecycleApprovalService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeLifecycleApprovalService(
            employeeRepository, requestRepository, eventPublisher
        );
    }

    @Test
    void selfServiceOffboardingCreatesPendingRequest() {
        Employee employee = employee("ACTIVE");
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));
        when(requestRepository.existsByEmployeeIdAndActionAndStatus(
            employee.getId(),
            EmployeeLifecycleAction.OFFBOARDING,
            EmployeeLifecycleStatus.PENDING
        )).thenReturn(false);
        when(requestRepository.save(any(EmployeeLifecycleRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.requestOwnOffboarding(
            "Worker@Example.com", "Leaving the company"
        );

        assertEquals("OFFBOARDING", response.action());
        assertEquals("PENDING", response.status());
        assertEquals("worker@example.com", response.requestedBy());
        verify(employeeRepository, never()).save(any(Employee.class));
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void hrApprovalTerminatesEmployeeAndRecordsReviewer() {
        Employee employee = employee("ACTIVE");
        EmployeeLifecycleRequest request = new EmployeeLifecycleRequest(
            employee.getId(),
            EmployeeLifecycleAction.OFFBOARDING,
            "worker@example.com",
            "Leaving the company"
        );
        when(requestRepository.findByIdForUpdate(request.getId()))
            .thenReturn(Optional.of(request));
        when(employeeRepository.findByIdForUpdate(employee.getId()))
            .thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(requestRepository.save(request)).thenReturn(request);

        var response = service.decide(
            request.getId(),
            "hr@example.com",
            "HR_ADMIN",
            new EmployeeLifecycleDecision(true, "Exit checklist complete")
        );

        assertEquals("TERMINATED", employee.getEmploymentStatus());
        assertEquals("APPROVED", response.status());
        assertEquals("hr@example.com", response.reviewedBy());
        assertEquals("Exit checklist complete", response.reviewNote());
        verify(employeeRepository).save(employee);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void onboardingApprovalActivatesEmployeeOnlyAfterReview() {
        Employee employee = employee("ONBOARDING");
        EmployeeLifecycleRequest request = new EmployeeLifecycleRequest(
            employee.getId(),
            EmployeeLifecycleAction.ONBOARDING,
            "worker@example.com",
            "Profile submitted"
        );
        when(requestRepository.findByIdForUpdate(request.getId()))
            .thenReturn(Optional.of(request));
        when(employeeRepository.findByIdForUpdate(employee.getId()))
            .thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(requestRepository.save(request)).thenReturn(request);

        service.decide(
            request.getId(),
            "hr@example.com",
            "HR_MANAGER",
            new EmployeeLifecycleDecision(true, null)
        );

        assertEquals("ACTIVE", employee.getEmploymentStatus());
        verify(employeeRepository).save(employee);
    }

    @Test
    void requesterCannotApproveOwnLifecycleRequest() {
        EmployeeLifecycleRequest request = new EmployeeLifecycleRequest(
            UUID.randomUUID(),
            EmployeeLifecycleAction.OFFBOARDING,
            "hr@example.com",
            "Offboarding"
        );
        when(requestRepository.findByIdForUpdate(request.getId()))
            .thenReturn(Optional.of(request));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.decide(
                request.getId(),
                "HR@example.com",
                "HR_ADMIN",
                new EmployeeLifecycleDecision(true, null)
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(employeeRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectionRequiresAnExplanationAndLeavesEmployeeUnchanged() {
        EmployeeLifecycleRequest request = new EmployeeLifecycleRequest(
            UUID.randomUUID(),
            EmployeeLifecycleAction.OFFBOARDING,
            "worker@example.com",
            "Leaving the company"
        );
        when(requestRepository.findByIdForUpdate(request.getId()))
            .thenReturn(Optional.of(request));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.decide(
                request.getId(),
                "hr@example.com",
                "HR_ADMIN",
                new EmployeeLifecycleDecision(false, " ")
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(employeeRepository, never()).findByIdForUpdate(any());
        verify(requestRepository, never()).save(any(EmployeeLifecycleRequest.class));
    }

    @Test
    void managerCannotReviewLifecycleRequests() {
        var exception = assertThrows(ResponseStatusException.class, () ->
            service.listPending("DEPARTMENT_MANAGER")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(requestRepository, never())
            .findByStatusOrderByRequestedAtAsc(any());
    }

    private static Employee employee(String status) {
        Employee employee = new Employee();
        employee.setEmail("worker@example.com");
        employee.setEmploymentStatus(status);
        employee.onCreate();
        return employee;
    }
}
