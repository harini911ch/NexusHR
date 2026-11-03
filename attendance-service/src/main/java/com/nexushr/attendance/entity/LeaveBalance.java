package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(
    name = "uq_leave_balance_employee_type_year",
    columnNames = {"employee_email", "leave_type_id", "balance_year"}
))
public class LeaveBalance {

    @Id
    private UUID id;

    @Column(name = "employee_email", nullable = false, length = 180)
    private String employeeEmail;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "balance_year", nullable = false)
    private int year;

    @Column(name = "granted_days", nullable = false)
    private int grantedDays;

    @Column(name = "used_days", nullable = false)
    private int usedDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeaveBalance() {}

    public LeaveBalance(String employeeEmail, UUID leaveTypeId, int year, int grantedDays) {
        this.id = UUID.randomUUID();
        this.employeeEmail = employeeEmail;
        this.leaveTypeId = leaveTypeId;
        this.year = year;
        this.grantedDays = grantedDays;
        this.usedDays = 0;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getEmployeeEmail() { return employeeEmail; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public int getYear() { return year; }
    public int getGrantedDays() { return grantedDays; }
    public int getUsedDays() { return usedDays; }
    public int getRemainingDays() { return Math.max(0, grantedDays - usedDays); }

    public void addUsedDays(int days) {
        if (days < 0 || usedDays + days > grantedDays) {
            throw new IllegalArgumentException("Leave exceeds the available balance");
        }
        usedDays += days;
    }

    public void updateGrant(int days) {
        if (days < usedDays) {
            throw new IllegalArgumentException("Allowance cannot be less than leave already used");
        }
        grantedDays = days;
    }
}
