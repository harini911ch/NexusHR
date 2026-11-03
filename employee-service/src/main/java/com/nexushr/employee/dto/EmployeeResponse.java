package com.nexushr.employee.dto;

import com.nexushr.employee.entity.Employee;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    String employeeCode,
    String fullName,
    String email,
    String department,
    UUID departmentId,
    String jobTitle,
    UUID designationId,
    String employmentType,
    String employmentStatus,
    LocalDate hireDate,
    String phoneNumber,
    String managerEmail,
    Instant createdAt,
    Instant updatedAt
) {
    public static EmployeeResponse from(
        Employee employee, String departmentName, String designationTitle
    ) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getEmployeeCode(),
            employee.getFullName(),
            employee.getEmail(),
            departmentName,
            employee.getDepartmentId(),
            designationTitle,
            employee.getDesignationId(),
            employee.getEmploymentType(),
            employee.getEmploymentStatus(),
            employee.getHireDate(),
            employee.getPhoneNumber(),
            employee.getManagerEmail(),
            employee.getCreatedAt(),
            employee.getUpdatedAt()
        );
    }
}
