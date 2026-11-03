package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PerformanceGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerformanceGoalRepository extends JpaRepository<PerformanceGoal, UUID> {
    List<PerformanceGoal> findByEmployeeEmailOrderByCreatedAtDesc(String email);
    Optional<PerformanceGoal> findByIdAndEmployeeEmail(UUID id, String email);
}