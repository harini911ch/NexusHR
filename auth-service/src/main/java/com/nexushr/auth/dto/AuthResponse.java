package com.nexushr.auth.dto;

import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String fullName,
    String email,
    String role
) {}