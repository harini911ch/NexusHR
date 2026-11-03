package com.nexushr.auth.dto;

import com.nexushr.auth.entity.Role;
import java.time.Instant;

public record InvitationResponse(
    String email,
    Role role,
    String invitationToken,
    Instant expiresAt
) {}
