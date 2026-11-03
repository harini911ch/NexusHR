package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "employee_documents",
    indexes = {
        @Index(name = "idx_employee_documents_employee", columnList = "employee_id"),
        @Index(name = "idx_employee_documents_uploaded_at", columnList = "uploaded_at")
    }
)
public class EmployeeDocument {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_by", nullable = false, length = 180)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected EmployeeDocument() {}

    public EmployeeDocument(
        UUID employeeId,
        String fileName,
        String contentType,
        long fileSize,
        String storageKey,
        String sha256,
        String uploadedBy
    ) {
        this.id = UUID.randomUUID();
        this.employeeId = employeeId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public String getStorageKey() { return storageKey; }
    public String getSha256() { return sha256; }
    public String getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
}
