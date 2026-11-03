package com.nexushr.insights.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SkillGapRequest(
    @NotBlank @Email String employeeEmail,
    @NotBlank @Size(max = 150) String targetRole,
    @NotEmpty @Size(max = 50) Set<@NotBlank @Size(max = 80) String> currentSkills,
    @NotEmpty @Size(max = 50) Set<@NotBlank @Size(max = 80) String> requiredSkills
) {}