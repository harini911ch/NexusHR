package com.nexushr.employee.service;

import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.employee.dto.*;
import com.nexushr.employee.entity.*;
import com.nexushr.employee.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {

    private static final Set<String> HR_ROLES =
        Set.of("SUPER_ADMIN", "HR_ADMIN", "HR_MANAGER");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeLifecycleRequestRepository lifecycleRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeService(
        EmployeeRepository employeeRepository,
        DepartmentRepository departmentRepository,
        DesignationRepository designationRepository,
        EmployeeLifecycleRequestRepository lifecycleRequestRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.lifecycleRequestRepository = lifecycleRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EmployeeResponse createMyProfile(String email, EmployeeRequest request) {
        String normalizedEmail = normalizeEmail(email);
        if (employeeRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Employee profile already exists"
            );
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(nextEmployeeCode());
        employee.setEmail(normalizedEmail);
        employee.setEmploymentStatus("ONBOARDING");
        apply(employee, request, false);
        Employee saved = employeeRepository.save(employee);
        createOnboardingApproval(saved, normalizedEmail);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse createDirectoryProfile(
        EmployeeRequest request, String createdBy
    ) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Email is required when creating an employee"
            );
        }
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "An employee profile already exists for this email"
            );
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(nextEmployeeCode());
        employee.setEmail(email);
        employee.setEmploymentStatus("ONBOARDING");
        apply(employee, request, true);
        Employee saved = employeeRepository.save(employee);
        createOnboardingApproval(saved, normalizeEmail(createdBy));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getByEmail(String email) {
        return toResponse(employeeRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee profile not found"
            )));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(
        String query,
        UUID departmentId,
        String status,
        int page,
        int size,
        String sortBy,
        String direction,
        String viewerEmail,
        String viewerRole
    ) {
        if (!HR_ROLES.contains(viewerRole) &&
            !Set.of("MANAGER", "DEPARTMENT_MANAGER").contains(viewerRole)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot access the employee directory"
            );
        }

        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size, 100));
        String safeSort = Set.of("fullName", "email", "employeeCode", "hireDate",
            "createdAt", "employmentStatus").contains(sortBy) ? sortBy : "fullName";
        Sort sort = "desc".equalsIgnoreCase(direction)
            ? Sort.by(safeSort).descending()
            : Sort.by(safeSort).ascending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);

        Specification<Employee> spec = Specification.where(null);
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, criteria, builder) -> builder.or(
                builder.like(builder.lower(root.get("fullName")), pattern),
                builder.like(builder.lower(root.get("email")), pattern),
                builder.like(builder.lower(root.get("employeeCode")), pattern)
            ));
        }
        if (departmentId != null) {
            spec = spec.and((root, criteria, builder) ->
                builder.equal(root.get("departmentId"), departmentId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, criteria, builder) ->
                builder.equal(root.get("employmentStatus"),
                    status.trim().toUpperCase(Locale.ROOT)));
        }

        if ("MANAGER".equals(viewerRole)) {
            spec = spec.and((root, criteria, builder) ->
                builder.equal(builder.lower(root.get("managerEmail")),
                    normalizeEmail(viewerEmail)));
        } else if ("DEPARTMENT_MANAGER".equals(viewerRole)) {
            List<UUID> managedDepartments = departmentRepository
                .findByManagerEmailIgnoreCaseAndActiveTrue(viewerEmail).stream()
                .map(Department::getId)
                .toList();
            if (managedDepartments.isEmpty()) return Page.empty(pageable);
            spec = spec.and((root, criteria, builder) ->
                root.get("departmentId").in(managedDepartments));
        }

        return employeeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeAnalyticsResponse analytics(String viewerEmail, String viewerRole) {
        List<EmployeeRepository.StatusCount> statuses;
        List<EmployeeRepository.DepartmentCount> departments;
        if (HR_ROLES.contains(viewerRole)) {
            statuses = employeeRepository.countByStatus();
            departments = employeeRepository.countByDepartment();
        } else if ("MANAGER".equals(viewerRole)) {
            statuses = employeeRepository.countDirectReportsByStatus(viewerEmail);
            departments = employeeRepository.countDirectReportsByDepartment(viewerEmail);
        } else if ("DEPARTMENT_MANAGER".equals(viewerRole)) {
            List<UUID> departmentIds = departmentRepository
                .findByManagerEmailIgnoreCaseAndActiveTrue(viewerEmail).stream()
                .map(Department::getId).toList();
            if (departmentIds.isEmpty()) {
                return new EmployeeAnalyticsResponse(0, List.of(), List.of());
            }
            statuses = employeeRepository.countByDepartmentsStatus(departmentIds);
            departments = employeeRepository.countByDepartments(departmentIds);
        } else {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot access workforce analytics"
            );
        }

        Map<UUID, String> departmentNames = departmentRepository.findAllById(
            departments.stream().map(EmployeeRepository.DepartmentCount::getDepartmentId).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(
            Department::getId, Department::getName
        ));
        List<EmployeeAnalyticsResponse.StatusCount> statusBreakdown = statuses.stream()
            .map(status -> new EmployeeAnalyticsResponse.StatusCount(
                status.getStatus(), status.getTotal()
            )).toList();
        List<EmployeeAnalyticsResponse.DepartmentCount> departmentBreakdown = departments.stream()
            .map(department -> new EmployeeAnalyticsResponse.DepartmentCount(
                departmentNames.getOrDefault(department.getDepartmentId(), "Unassigned"),
                department.getTotal()
            )).toList();
        long total = statusBreakdown.stream().mapToLong(
            EmployeeAnalyticsResponse.StatusCount::count
        ).sum();
        return new EmployeeAnalyticsResponse(total, statusBreakdown, departmentBreakdown);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id, String viewerEmail, String viewerRole) {
        Employee employee = findById(id);
        ensureCanView(employee, viewerEmail, viewerRole);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        Employee employee = findById(id);
        apply(employee, request, true);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse changeStatus(UUID id, String status) {
        Employee employee = findById(id);
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        Set<String> allowedStatuses =
            Set.of("ONBOARDING", "ACTIVE", "ON_LEAVE", "TERMINATED");
        if (!allowedStatuses.contains(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unsupported employee status"
            );
        }
        if ("TERMINATED".equals(employee.getEmploymentStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A terminated employee cannot be reactivated"
            );
        }
        if ("ONBOARDING".equals(employee.getEmploymentStatus()) ||
            "TERMINATED".equals(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Onboarding and offboarding status changes require an approval workflow"
            );
        }
        employee.setEmploymentStatus(normalized);
        return toResponse(employeeRepository.save(employee));
    }

    private void createOnboardingApproval(Employee employee, String requestedBy) {
        EmployeeLifecycleRequest request = lifecycleRequestRepository.save(
            new EmployeeLifecycleRequest(
            employee.getId(),
            EmployeeLifecycleAction.ONBOARDING,
            requestedBy,
            "Employee profile created; HR approval is required before activation"
            )
        );
        eventPublisher.publishEvent(new NotificationEvent(
            employee.getEmail(),
            "Onboarding request submitted",
            "Your employee profile is pending HR approval.",
            "EMPLOYEE_LIFECYCLE",
            "EMPLOYEE_LIFECYCLE_REQUEST",
            request.getId().toString()
        ));
    }

    private void apply(Employee employee, EmployeeRequest request, boolean allowManager) {
        employee.setFullName(request.fullName().trim());
        employee.setEmploymentType(request.employmentType().trim().toUpperCase(Locale.ROOT));
        employee.setHireDate(request.hireDate());
        employee.setPhoneNumber(request.phoneNumber());

        Department department = departmentRepository.findByNameIgnoreCase(
            request.department().trim()
        ).orElseGet(() -> departmentRepository.save(new Department(
            request.department().trim(), null, null, null
        )));

        Designation designation = designationRepository
            .findByTitleIgnoreCaseAndDepartmentId(
                request.jobTitle().trim(), department.getId()
            )
            .orElseGet(() -> designationRepository.save(new Designation(
                request.jobTitle().trim(), department.getId(), null
            )));

        if (!designation.isActive() || !department.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Employee department and designation must be active"
            );
        }

        employee.setDepartmentId(department.getId());
        employee.setDesignationId(designation.getId());
        if (allowManager) {
            assignManager(employee, request.managerEmail());
        }
    }

    private void assignManager(Employee employee, String managerEmail) {
        if (managerEmail == null || managerEmail.isBlank()) {
            employee.setManagerEmail(null);
            employee.setManagerEmployeeId(null);
            return;
        }

        Employee manager = employeeRepository.findByEmailIgnoreCase(
            normalizeEmail(managerEmail)
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Reporting manager is not an active employee"
        ));
        if (manager.getId() != null && manager.getId().equals(employee.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "An employee cannot report to themselves"
            );
        }
        if ("TERMINATED".equals(manager.getEmploymentStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A terminated employee cannot be a reporting manager"
            );
        }
        employee.setManagerEmail(manager.getEmail());
        employee.setManagerEmployeeId(manager.getId());
    }

    private void ensureCanView(Employee employee, String viewerEmail, String viewerRole) {
        if (HR_ROLES.contains(viewerRole)) return;
        if ("MANAGER".equals(viewerRole) &&
            normalizeEmail(viewerEmail).equals(normalizeEmail(employee.getManagerEmail()))) {
            return;
        }
        if ("DEPARTMENT_MANAGER".equals(viewerRole) &&
            departmentRepository.findByManagerEmailIgnoreCaseAndActiveTrue(viewerEmail)
                .stream().anyMatch(department ->
                    department.getId().equals(employee.getDepartmentId()))) {
            return;
        }
        if (normalizeEmail(viewerEmail).equals(normalizeEmail(employee.getEmail()))) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee access denied");
    }

    private Employee findById(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));
    }

    private EmployeeResponse toResponse(Employee employee) {
        Department department = departmentRepository.findById(employee.getDepartmentId())
            .orElseThrow(() -> new IllegalStateException(
                "Employee references a missing department"
            ));
        Designation designation = designationRepository.findById(employee.getDesignationId())
            .orElseThrow(() -> new IllegalStateException(
                "Employee references a missing designation"
            ));
        return EmployeeResponse.from(employee, department.getName(), designation.getTitle());
    }

    private String nextEmployeeCode() {
        return "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
