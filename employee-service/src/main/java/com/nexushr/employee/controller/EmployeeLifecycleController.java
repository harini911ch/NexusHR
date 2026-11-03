package com.nexushr.employee.controller;

import com.nexushr.employee.dto.EmployeeLifecycleDecision;
import com.nexushr.employee.dto.EmployeeLifecycleRequestCreate;
import com.nexushr.employee.dto.EmployeeLifecycleRequestResponse;
import com.nexushr.employee.dto.EmployeeOffboardingRequest;
import com.nexushr.employee.service.EmployeeLifecycleApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeLifecycleController {

    private final EmployeeLifecycleApprovalService service;

    public EmployeeLifecycleController(EmployeeLifecycleApprovalService service) {
        this.service = service;
    }

    @PostMapping("/me/lifecycle-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeLifecycleRequestResponse requestOwnOffboarding(
        Authentication authentication,
        @Valid @RequestBody EmployeeOffboardingRequest request
    ) {
        return service.requestOwnOffboarding(
            authentication.getName(), request.reason()
        );
    }

    @GetMapping("/me/lifecycle-requests")
    public List<EmployeeLifecycleRequestResponse> myRequests(
        Authentication authentication
    ) {
        return service.listOwn(authentication.getName());
    }

    @PostMapping("/{employeeId}/lifecycle-requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeLifecycleRequestResponse requestForEmployee(
        @PathVariable UUID employeeId,
        Authentication authentication,
        @Valid @RequestBody EmployeeLifecycleRequestCreate request
    ) {
        return service.requestForEmployee(
            employeeId, authentication.getName(), role(authentication), request
        );
    }

    @GetMapping("/lifecycle-requests/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public List<EmployeeLifecycleRequestResponse> pendingRequests(
        Authentication authentication
    ) {
        return service.listPending(role(authentication));
    }

    @PostMapping("/lifecycle-requests/{requestId}/decision")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public EmployeeLifecycleRequestResponse decide(
        @PathVariable UUID requestId,
        Authentication authentication,
        @Valid @RequestBody EmployeeLifecycleDecision decision
    ) {
        return service.decide(
            requestId, authentication.getName(), role(authentication), decision
        );
    }

    private static String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
            .map(authority -> authority.getAuthority().substring(5))
            .findFirst()
            .orElse("EMPLOYEE");
    }
}
