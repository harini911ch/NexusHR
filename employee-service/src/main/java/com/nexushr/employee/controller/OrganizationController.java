package com.nexushr.employee.controller;

import com.nexushr.employee.dto.*;
import com.nexushr.employee.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping("/api/departments")
    public List<DepartmentResponse> departments(
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.departments(includeInactive);
    }

    @PostMapping("/api/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(
        @Valid @RequestBody DepartmentRequest request
    ) {
        return service.createDepartment(request);
    }

    @PutMapping("/api/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public DepartmentResponse updateDepartment(
        @PathVariable UUID id,
        @Valid @RequestBody DepartmentRequest request
    ) {
        return service.updateDepartment(id, request);
    }

    @GetMapping("/api/designations")
    public List<DesignationResponse> designations(
        @RequestParam(required = false) UUID departmentId,
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.designations(departmentId, includeInactive);
    }

    @PostMapping("/api/designations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public DesignationResponse createDesignation(
        @Valid @RequestBody DesignationRequest request
    ) {
        return service.createDesignation(request);
    }

    @PutMapping("/api/designations/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public DesignationResponse updateDesignation(
        @PathVariable UUID id,
        @Valid @RequestBody DesignationRequest request
    ) {
        return service.updateDesignation(id, request);
    }
}
