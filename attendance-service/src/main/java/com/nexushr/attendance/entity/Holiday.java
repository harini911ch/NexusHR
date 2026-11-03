package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(length = 180)
    private String location;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Holiday() {}

    public Holiday(String name, LocalDate date, String location) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.date = date;
        this.location = location;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public LocalDate getDate() { return date; }
    public String getLocation() { return location; }
    public Instant getCreatedAt() { return createdAt; }
}
