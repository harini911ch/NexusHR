package com.nexushr.auth.dto;

import com.nexushr.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvitationRequest(
    @NotBlank @Email @Size(max = 180) String email,
    @NotNull Role role
) {}
