package com.nexushr.employee.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "app.documents.storage",
    havingValue = "local",
    matchIfMissing = true
)
public class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalDocumentStorage(
        @Value("${app.documents.local-directory:./data/employee-documents}")
        String directory
    ) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not store employee document", exception);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not read employee document", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not delete employee document", exception);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid document storage key");
        }
        return target;
    }
}
