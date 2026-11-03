package com.nexushr.performance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackInvitationRequest(
    @NotBlank @Email @Size(max = 180) String inviteeEmail,
    @NotBlank String relationship
) {}
