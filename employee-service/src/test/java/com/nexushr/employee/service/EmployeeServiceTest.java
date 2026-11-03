package com.nexushr.employee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.employee.dto.EmployeeRequest;
import com.nexushr.employee.entity.Department;
import com.nexushr.employee.entity.Designation;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.entity.EmployeeLifecycleRequest;
import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.employee.repository.DepartmentRepository;
import com.nexushr.employee.repository.DesignationRepository;
import com.nexushr.employee.repository.EmployeeLifecycleRequestRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import java.time.LocalDate;
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
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DesignationRepository designationRepository;
    @Mock private EmployeeLifecycleRequestRepository lifecycleRequestRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(
            employeeRepository,
            departmentRepository,
            designationRepository,
            lifecycleRequestRepository,
            eventPublisher
        );
    }

    @Test
    void createsOwnProfileAsOnboardingAndReturnsDto() {
        Department department = new Department("Engineering", null, null, null);
        Designation designation = new Designation(
            "Engineer", UUID.randomUUID(), null
        );
        when(employeeRepository.existsByEmailIgnoreCase("worker@example.com"))
            .thenReturn(false);
        when(departmentRepository.findByNameIgnoreCase("Engineering"))
            .thenReturn(Optional.of(department));
        when(designationRepository.findByTitleIgnoreCaseAndDepartmentId(
            "Engineer", department.getId()))
            .thenReturn(Optional.of(designation));
        when(employeeRepository.save(any(Employee.class)))
            .thenAnswer(invocation -> {
                Employee employee = invocation.getArgument(0);
                employee.onCreate();
                return employee;
            });
        when(lifecycleRequestRepository.save(any(EmployeeLifecycleRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(departmentRepository.findById(department.getId()))
            .thenReturn(Optional.of(department));
        when(designationRepository.findById(designation.getId()))
            .thenReturn(Optional.of(designation));

        var response = service.createMyProfile(
            "Worker@Example.com",
            request(null, null)
        );

        assertEquals("worker@example.com", response.email());
        assertEquals("Engineering", response.department());
        assertEquals("Engineer", response.jobTitle());
        assertEquals("ONBOARDING", response.employmentStatus());
        assertEquals(null, response.managerEmail());
        verify(lifecycleRequestRepository).save(any());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void preventsCreatingDuplicateOwnProfile() {
        when(employeeRepository.existsByEmailIgnoreCase("worker@example.com"))
            .thenReturn(true);

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.createMyProfile("worker@example.com", request(null, null))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void onboardingCannotBeActivatedWithoutApproval() {
        Employee employee = new Employee();
        employee.setEmploymentStatus("ONBOARDING");
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.changeStatus(employeeId, "ACTIVE")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void managerCannotAccessAnotherManagersReport() {
        Department department = new Department("Engineering", null, null, null);
        Designation designation = new Designation(
            "Engineer", UUID.randomUUID(), null
        );
        Employee employee = new Employee();
        employee.setEmail("report@example.com");
        employee.setDepartmentId(department.getId());
        employee.setDesignationId(designation.getId());
        employee.setManagerEmail("other.manager@example.com");
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.getById(employeeId, "manager@example.com", "MANAGER")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(departmentRepository, never()).findById(any());
    }

    private static EmployeeRequest request(String email, String managerEmail) {
        return new EmployeeRequest(
            "Worker Name", "Engineering", "Engineer", "FULL_TIME",
            LocalDate.now().minusDays(1), "555-0100", email, managerEmail
        );
    }
}
