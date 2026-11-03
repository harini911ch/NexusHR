package com.nexushr.payroll.controller;

import com.nexushr.payroll.dto.PayrollProcessRequest;
import com.nexushr.payroll.dto.PayrollRunRequest;
import com.nexushr.payroll.dto.PayrollRunResponse;
import com.nexushr.payroll.dto.PayslipResponse;
import com.nexushr.payroll.dto.SalaryStructureRequest;
import com.nexushr.payroll.dto.SalaryStructureResponse;
import com.nexushr.payroll.service.PayrollService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {
    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    @PostMapping("/salary-structures")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public SalaryStructureResponse createSalaryStructure(
        @Valid @RequestBody SalaryStructureRequest request
    ) {
        return SalaryStructureResponse.from(service.createSalaryStructure(request));
    }

    @GetMapping("/salary-structures")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public List<SalaryStructureResponse> salaryStructures() {
        return service.salaryStructures();
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayslipResponse process(
        @Valid @RequestBody PayrollProcessRequest request,
        Authentication authentication
    ) {
        return PayslipResponse.from(service.process(request, authentication.getName()));
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayrollRunResponse createRun(
        @Valid @RequestBody PayrollRunRequest request,
        Authentication authentication
    ) {
        return service.createRun(request, authentication.getName());
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public List<PayrollRunResponse> runs() {
        return service.runs();
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayrollRunResponse getRun(@PathVariable UUID id) {
        return service.getRun(id);
    }

    @GetMapping("/runs/{id}/payslips")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public List<PayslipResponse> runPayslips(@PathVariable UUID id) {
        return service.getRunPayslips(id);
    }

    @PutMapping("/runs/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayrollRunResponse approveRun(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        return service.approveRun(id, authentication.getName());
    }

    @GetMapping("/runs/{id}/export.csv")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportRun(@PathVariable UUID id) {
        byte[] content = service.exportRunCsv(id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"payroll-run-" + id + ".csv\"")
            .body(content);
    }

    @GetMapping("/runs/{id}/export.xlsx")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportRunXlsx(@PathVariable UUID id) {
        byte[] content = service.exportRunXlsx(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"payroll-run-" + id + ".xlsx\"")
            .body(content);
    }

    @GetMapping("/runs/{id}/export.pdf")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportRunPdf(@PathVariable UUID id) {
        byte[] content = service.exportRunPdf(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"payroll-run-" + id + ".pdf\"")
            .body(content);
    }

    @GetMapping("/me")
    public List<PayslipResponse> myPayslips(Authentication authentication) {
        return service.getMyPayslips(authentication.getName());
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayslipResponse getPayslip(@PathVariable UUID id) {
        return service.getPayslip(id);
    }

    @PutMapping("/payslips/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN','PAYROLL_ADMIN','SUPER_ADMIN')")
    public PayslipResponse approvePayslip(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        return service.approvePayslip(id, authentication.getName());
    }
}