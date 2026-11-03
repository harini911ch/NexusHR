package com.nexushr.employee.service;

public record DocumentDownload(
    String fileName,
    String contentType,
    byte[] content
) {}
