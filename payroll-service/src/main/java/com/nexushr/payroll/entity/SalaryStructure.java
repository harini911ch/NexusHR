package com.nexushr.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_structures")
public class SalaryStructure {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "basic_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "housing_allowance", nullable = false, precision = 14, scale = 2)
    private BigDecimal housingAllowance = BigDecimal.ZERO;

    @Column(name = "transport_allowance", nullable = false, precision = 14, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowance", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "other_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID v) { employeeId = v; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String v) { employeeCode = v; }
    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String v) { employeeEmail = v; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal v) { basicSalary = v; }
    public BigDecimal getHousingAllowance() { return housingAllowance; }
    public void setHousingAllowance(BigDecimal v) { housingAllowance = v; }
    public BigDecimal getTransportAllowance() { return transportAllowance; }
    public void setTransportAllowance(BigDecimal v) { transportAllowance = v; }
    public BigDecimal getOtherAllowance() { return otherAllowance; }
    public void setOtherAllowance(BigDecimal v) { otherAllowance = v; }
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal v) { taxPercentage = v; }
    public BigDecimal getOtherDeduction() { return otherDeduction; }
    public void setOtherDeduction(BigDecimal v) { otherDeduction = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { currency = v; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate v) { effectiveFrom = v; }
}