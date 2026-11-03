package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private UUID id;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "designation_id", nullable = false)
    private UUID designationId;

    @Column(name = "manager_employee_id")
    private UUID managerEmployeeId;

    @Column(name = "manager_email", length = 180)
    private String managerEmail;

    @Column(name = "employment_type", nullable = false)
    private String employmentType;

    @Column(name = "employment_status", nullable = false)
    private String employmentStatus = "ACTIVE";

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String value) { employeeCode = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID value) { departmentId = value; }
    public UUID getDesignationId() { return designationId; }
    public void setDesignationId(UUID value) { designationId = value; }
    public UUID getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(UUID value) { managerEmployeeId = value; }
    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String value) { managerEmail = value; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String value) { employmentType = value; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String value) { employmentStatus = value; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate value) { hireDate = value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}