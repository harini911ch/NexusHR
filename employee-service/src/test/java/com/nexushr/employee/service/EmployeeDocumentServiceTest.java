package com.nexushr.employee.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.employee.entity.Employee;
import com.nexushr.employee.entity.EmployeeDocument;
import com.nexushr.employee.repository.EmployeeDocumentAuditRepository;
import com.nexushr.employee.repository.EmployeeDocumentRepository;
import com.nexushr.employee.repository.EmployeeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EmployeeDocumentServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeDocumentRepository documentRepository;
    @Mock private EmployeeDocumentAuditRepository auditRepository;
    @Mock private DocumentStorage storage;

    private EmployeeDocumentService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeDocumentService(
            employeeRepository, documentRepository, auditRepository, storage
        );
    }

    @Test
    void storesDocumentPrivatelyAndReturnsMetadataOnly() {
        Employee employee = employee();
        byte[] content = "%PDF-1.7\nsample".getBytes();
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));
        when(documentRepository.saveAndFlush(any(EmployeeDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.uploadOwn(
            "Worker@Example.com",
            new MockMultipartFile("file", "passport.pdf", "text/plain", content)
        );

        assertEquals(employee.getId(), response.employeeId());
        assertEquals("passport.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(content.length, response.fileSize());
        verify(storage).store(
            org.mockito.ArgumentMatchers.startsWith("employees/" + employee.getId() + "/"),
            eq(content),
            eq("application/pdf")
        );
        verify(auditRepository).save(any());
    }

    @Test
    void rejectsSpoofedContentAndUnsupportedFileTypes() {
        Employee employee = employee();
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.uploadOwn(
                "worker@example.com",
                new MockMultipartFile(
                    "file", "resume.pdf", "application/pdf", "not a pdf".getBytes()
                )
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(storage, never()).store(any(), any(), any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        Employee employee = employee();
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.uploadOwn(
                "worker@example.com",
                new MockMultipartFile("file", "large.pdf", "application/pdf", oversized)
            )
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatusCode());
        verify(storage, never()).store(any(), any(), any());
    }

    @Test
    void employeeCannotDownloadAnotherEmployeesDocument() {
        Employee employee = employee();
        UUID documentId = UUID.randomUUID();
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));
        when(documentRepository.findByIdAndEmployeeId(documentId, employee.getId()))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.downloadOwn(documentId, "worker@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(storage, never()).load(any());
    }

    @Test
    void onlyHrCanAccessAnotherEmployeesDocuments() {
        var exception = assertThrows(ResponseStatusException.class, () ->
            service.listForEmployee(UUID.randomUUID(), "DEPARTMENT_MANAGER")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(employeeRepository, never()).findById(any());
        verify(documentRepository, never()).findByEmployeeIdOrderByUploadedAtDesc(any());
    }

    @Test
    void downloadsStoredBytesAndAuditsTheRead() {
        Employee employee = employee();
        byte[] content = "%PDF-1.7\nsample".getBytes();
        EmployeeDocument document = new EmployeeDocument(
            employee.getId(),
            "offer.pdf",
            "application/pdf",
            content.length,
            "employees/" + employee.getId() + "/document.pdf",
            "0".repeat(64),
            "worker@example.com"
        );
        when(employeeRepository.findByEmailIgnoreCase("worker@example.com"))
            .thenReturn(Optional.of(employee));
        when(documentRepository.findByIdAndEmployeeId(
            document.getId(), employee.getId()
        )).thenReturn(Optional.of(document));
        when(storage.load(document.getStorageKey())).thenReturn(content);

        var download = service.downloadOwn(document.getId(), "worker@example.com");

        assertEquals("offer.pdf", download.fileName());
        assertEquals("application/pdf", download.contentType());
        assertArrayEquals(content, download.content());
        verify(auditRepository).save(any());
    }

    private static Employee employee() {
        Employee employee = new Employee();
        employee.setEmail("worker@example.com");
        employee.onCreate();
        return employee;
    }
}
