package com.nexushr.employee.controller;

import com.nexushr.employee.dto.*;
import com.nexushr.employee.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createMyProfile(
        Authentication authentication,
        @Valid @RequestBody EmployeeRequest request
    ) {
        return service.createMyProfile(authentication.getName(), request);
    }

    @GetMapping("/me")
    public EmployeeResponse myProfile(Authentication authentication) {
        return service.getByEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createDirectoryProfile(
        Authentication authentication,
        @Valid @RequestBody EmployeeRequest request
    ) {
        return service.createDirectoryProfile(request, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER')")
    public Page<EmployeeResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) UUID departmentId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "fullName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction,
        Authentication authentication
    ) {
        return service.search(
            q, departmentId, status, page, size, sortBy, direction,
            authentication.getName(), role(authentication)
        );
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER')")
    public EmployeeAnalyticsResponse analytics(Authentication authentication) {
        return service.analytics(authentication.getName(), role(authentication));
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        return service.getById(
            id, authentication.getName(), role(authentication)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public EmployeeResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody EmployeeRequest request
    ) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public EmployeeResponse changeStatus(
        @PathVariable UUID id,
        @Valid @RequestBody LifecycleRequest request
    ) {
        return service.changeStatus(id, request.status());
    }

    private static String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
            .map(authority -> authority.getAuthority().substring(5))
            .findFirst()
            .orElse("EMPLOYEE");
    }
}
