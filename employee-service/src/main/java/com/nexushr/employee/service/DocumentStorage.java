package com.nexushr.employee.service;

public interface DocumentStorage {
    void store(String key, byte[] content, String contentType);
    byte[] load(String key);
    void delete(String key);
}
