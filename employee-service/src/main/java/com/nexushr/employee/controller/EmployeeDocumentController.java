package com.nexushr.employee.controller;

import com.nexushr.employee.dto.EmployeeDocumentResponse;
import com.nexushr.employee.service.DocumentDownload;
import com.nexushr.employee.service.EmployeeDocumentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
public class EmployeeDocumentController {

    private final EmployeeDocumentService service;

    public EmployeeDocumentController(EmployeeDocumentService service) {
        this.service = service;
    }

    @PostMapping(path = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDocumentResponse uploadOwn(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
    ) {
        return service.uploadOwn(authentication.getName(), file);
    }

    @GetMapping("/me/documents")
    public List<EmployeeDocumentResponse> myDocuments(Authentication authentication) {
        return service.listOwn(authentication.getName());
    }

    @GetMapping("/me/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadOwn(
        @PathVariable UUID documentId,
        Authentication authentication
    ) {
        return asDownload(service.downloadOwn(documentId, authentication.getName()));
    }

    @PostMapping(
        path = "/{employeeId}/documents",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDocumentResponse uploadForEmployee(
        @PathVariable UUID employeeId,
        Authentication authentication,
        @RequestParam("file") MultipartFile file
    ) {
        return service.uploadForEmployee(
            employeeId, authentication.getName(), role(authentication), file
        );
    }

    @GetMapping("/{employeeId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public List<EmployeeDocumentResponse> documentsForEmployee(
        @PathVariable UUID employeeId,
        Authentication authentication
    ) {
        return service.listForEmployee(employeeId, role(authentication));
    }

    @GetMapping("/{employeeId}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public ResponseEntity<byte[]> downloadForEmployee(
        @PathVariable UUID employeeId,
        @PathVariable UUID documentId,
        Authentication authentication
    ) {
        return asDownload(service.downloadForEmployee(
            employeeId, documentId, authentication.getName(), role(authentication)
        ));
    }

    private static ResponseEntity<byte[]> asDownload(DocumentDownload download) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.content().length)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(download.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore())
            .body(download.content());
    }

    private static String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
            .map(authority -> authority.getAuthority().substring(5))
            .findFirst()
            .orElse("EMPLOYEE");
    }
}
