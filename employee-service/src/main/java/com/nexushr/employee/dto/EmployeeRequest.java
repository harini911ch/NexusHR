package com.nexushr.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;

public record EmployeeRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Size(max = 100) String department,
    @NotBlank @Size(max = 120) String jobTitle,
    @NotBlank String employmentType,
    @NotNull LocalDate hireDate,
    @Size(max = 25) String phoneNumber,
    @Email @Size(max = 180) String email,
    @Email @Size(max = 180) String managerEmail
) {}