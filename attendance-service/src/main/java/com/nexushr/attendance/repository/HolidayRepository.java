package com.nexushr.attendance.repository;

import com.nexushr.attendance.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
    boolean existsByDateAndLocation(LocalDate date, String location);
}
