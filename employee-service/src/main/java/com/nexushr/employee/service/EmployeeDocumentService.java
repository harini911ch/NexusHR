package com.nexushr.employee.service;

import com.nexushr.employee.dto.EmployeeDocumentResponse;
import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.entity.EmployeeDocument;
import com.nexushr.employee.entity.EmployeeDocumentAuditEvent;
import com.nexushr.employee.repository.EmployeeDocumentAuditRepository;
import com.nexushr.employee.repository.EmployeeDocumentRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeDocumentService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(EmployeeDocumentService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> HR_ROLES =
        Set.of("SUPER_ADMIN", "HR_ADMIN", "HR_MANAGER");

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeDocumentAuditRepository auditRepository;
    private final DocumentStorage storage;

    public EmployeeDocumentService(
        EmployeeRepository employeeRepository,
        EmployeeDocumentRepository documentRepository,
        EmployeeDocumentAuditRepository auditRepository,
        DocumentStorage storage
    ) {
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.auditRepository = auditRepository;
        this.storage = storage;
    }

    @Transactional
    public EmployeeDocumentResponse uploadOwn(
        String actorEmail, MultipartFile file
    ) {
        Employee employee = findByEmail(actorEmail);
        return upload(employee, actorEmail, file);
    }

    @Transactional
    public EmployeeDocumentResponse uploadForEmployee(
        UUID employeeId, String actorEmail, String actorRole, MultipartFile file
    ) {
        requireHr(actorRole);
        return upload(findEmployee(employeeId), actorEmail, file);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> listOwn(String actorEmail) {
        Employee employee = findByEmail(actorEmail);
        return list(employee.getId());
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> listForEmployee(
        UUID employeeId, String actorRole
    ) {
        requireHr(actorRole);
        findEmployee(employeeId);
        return list(employeeId);
    }

    @Transactional
    public DocumentDownload downloadOwn(UUID documentId, String actorEmail) {
        Employee employee = findByEmail(actorEmail);
        return download(documentId, employee.getId(), actorEmail);
    }

    @Transactional
    public DocumentDownload downloadForEmployee(
        UUID employeeId, UUID documentId, String actorEmail, String actorRole
    ) {
        requireHr(actorRole);
        findEmployee(employeeId);
        return download(documentId, employeeId, actorEmail);
    }

    private EmployeeDocumentResponse upload(
        Employee employee, String actorEmail, MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Choose a non-empty PDF, PNG, or JPEG document"
            );
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE, "Employee documents may not exceed 10 MB"
            );
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Uploaded document could not be read", exception
            );
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE, "Employee documents may not exceed 10 MB"
            );
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String contentType = detectContentType(content);
        validateExtension(fileName, contentType);
        String storageKey = "employees/" + employee.getId() + "/" +
            UUID.randomUUID() + extensionFor(contentType);
        storage.store(storageKey, content, contentType);
        registerRollbackCleanup(storageKey);

        EmployeeDocument document = documentRepository.saveAndFlush(
            new EmployeeDocument(
                employee.getId(),
                fileName,
                contentType,
                content.length,
                storageKey,
                sha256(content),
                normalizeEmail(actorEmail)
            )
        );
        auditRepository.save(new EmployeeDocumentAuditEvent(
            document.getId(), employee.getId(), normalizeEmail(actorEmail), "UPLOAD"
        ));
        return EmployeeDocumentResponse.from(document);
    }

    private DocumentDownload download(
        UUID documentId, UUID employeeId, String actorEmail
    ) {
        EmployeeDocument document = documentRepository
            .findByIdAndEmployeeId(documentId, employeeId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee document not found"
            ));

        byte[] content = storage.load(document.getStorageKey());
        auditRepository.save(new EmployeeDocumentAuditEvent(
            document.getId(),
            employeeId,
            normalizeEmail(actorEmail),
            "DOWNLOAD"
        ));
        return new DocumentDownload(
            document.getFileName(), document.getContentType(), content
        );
    }

    private List<EmployeeDocumentResponse> list(UUID employeeId) {
        return documentRepository.findByEmployeeIdOrderByUploadedAtDesc(employeeId)
            .stream()
            .map(EmployeeDocumentResponse::from)
            .toList();
    }

    private Employee findByEmail(String email) {
        return employeeRepository.findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee profile not found"
            ));
    }

    private Employee findEmployee(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"
            ));
    }

    private void requireHr(String role) {
        if (!HR_ROLES.contains(role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only HR administrators can access employee documents"
            );
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            storage.delete(storageKey);
                        } catch (DocumentStorageException exception) {
                            LOGGER.error(
                                "Could not remove employee document after database rollback",
                                exception
                            );
                        }
                    }
                }
            }
        );
    }

    private static String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A document file name is required"
            );
        }
        String normalized = originalName.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
            .replaceAll("[\\p{Cntrl}]", "")
            .trim();
        if (normalized.isBlank() || normalized.length() > 255) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Document file name is invalid"
            );
        }
        return normalized;
    }

    private static String detectContentType(byte[] content) {
        if (startsWith(content, new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d})) {
            return "application/pdf";
        }
        if (startsWith(content, new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        })) {
            return "image/png";
        }
        if (startsWith(content, new byte[] {
            (byte) 0xff, (byte) 0xd8, (byte) 0xff
        })) {
            return "image/jpeg";
        }
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Only valid PDF, PNG, or JPEG files are accepted"
        );
    }

    private static void validateExtension(String fileName, String contentType) {
        String expectedExtension = extensionFor(contentType);
        if ("image/jpeg".equals(contentType) &&
            fileName.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
            return;
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(expectedExtension)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The document file extension does not match its content"
            );
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> throw new IllegalArgumentException("Unsupported document content type");
        };
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }
        return true;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
