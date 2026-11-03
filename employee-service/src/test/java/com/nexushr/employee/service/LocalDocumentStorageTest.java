package com.nexushr.employee.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDocumentStorageTest {

    @TempDir
    Path directory;

    @Test
    void storesAndLoadsDocumentsBelowTheConfiguredPrivateDirectory() {
        LocalDocumentStorage storage = new LocalDocumentStorage(directory.toString());
        byte[] content = "%PDF-1.7\nsample".getBytes();

        storage.store("employees/employee-id/document-id.pdf", content, "application/pdf");

        assertArrayEquals(
            content,
            storage.load("employees/employee-id/document-id.pdf")
        );
    }

    @Test
    void rejectsPathTraversalKeys() {
        LocalDocumentStorage storage = new LocalDocumentStorage(directory.toString());

        assertThrows(IllegalArgumentException.class, () ->
            storage.load("../outside.pdf")
        );
    }
}
