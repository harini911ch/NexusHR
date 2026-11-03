package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_employee_date",
        columnNames = {"employee_email", "attendance_date"}))
public class AttendanceRecord {

    @Id
    private UUID id;

    @Column(name = "employee_email", nullable = false, length = 180)
    private String employeeEmail;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in", nullable = false)
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AttendanceRecord() {}

    public AttendanceRecord(String employeeEmail, Instant checkIn, LocalDate attendanceDate,
                            boolean late) {
        this.id = UUID.randomUUID();
        this.employeeEmail = employeeEmail;
        this.attendanceDate = attendanceDate;
        this.checkIn = checkIn;
        this.status = late ? "LATE" : "PRESENT";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmployeeEmail() { return employeeEmail; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public Instant getCheckIn() { return checkIn; }
    public Instant getCheckOut() { return checkOut; }
    public String getStatus() { return status; }

    public void checkOut(Instant time) {
        if (checkOut != null) {
            throw new IllegalStateException("You have already checked out today");
        }
        if (time.isBefore(checkIn)) {
            throw new IllegalStateException("Check-out cannot be before check-in");
        }
        this.checkOut = time;
    }
}