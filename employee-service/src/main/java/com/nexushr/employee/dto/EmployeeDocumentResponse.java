package com.nexushr.employee.dto;

import com.nexushr.employee.entity.EmployeeDocument;
import java.time.Instant;
import java.util.UUID;

public record EmployeeDocumentResponse(
    UUID id,
    UUID employeeId,
    String fileName,
    String contentType,
    long fileSize,
    String uploadedBy,
    Instant uploadedAt
) {
    public static EmployeeDocumentResponse from(EmployeeDocument document) {
        return new EmployeeDocumentResponse(
            document.getId(),
            document.getEmployeeId(),
            document.getFileName(),
            document.getContentType(),
            document.getFileSize(),
            document.getUploadedBy(),
            document.getUploadedAt()
        );
    }
}
