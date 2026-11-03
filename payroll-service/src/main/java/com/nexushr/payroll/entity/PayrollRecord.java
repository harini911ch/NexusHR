package com.nexushr.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_records")
public class PayrollRecord {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "payroll_run_id")
    private UUID payrollRunId;

    @Column(name = "processed_by", length = 180)
    private String processedBy;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "basic_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "total_allowances", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAllowances;

    @Column(name = "gross_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "tax_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxDeduction;

    @Column(name = "other_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherDeduction;

    @Column(name = "total_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal netSalary;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING_APPROVAL";

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID v) { employeeId = v; }
    public UUID getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(UUID v) { payrollRunId = v; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String v) { processedBy = v; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String v) { employeeCode = v; }
    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String v) { employeeEmail = v; }
    public Integer getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(Integer v) { payrollMonth = v; }
    public Integer getPayrollYear() { return payrollYear; }
    public void setPayrollYear(Integer v) { payrollYear = v; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal v) { basicSalary = v; }
    public BigDecimal getTotalAllowances() { return totalAllowances; }
    public void setTotalAllowances(BigDecimal v) { totalAllowances = v; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal v) { grossSalary = v; }
    public BigDecimal getTaxDeduction() { return taxDeduction; }
    public void setTaxDeduction(BigDecimal v) { taxDeduction = v; }
    public BigDecimal getOtherDeduction() { return otherDeduction; }
    public void setOtherDeduction(BigDecimal v) { otherDeduction = v; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal v) { totalDeductions = v; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal v) { netSalary = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { currency = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { paymentStatus = v; }
    public Instant getProcessedAt() { return processedAt; }
}