package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    List<PerformanceReview> findByEmployeeEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmployeeEmailAndReviewPeriod(String employeeEmail, String reviewPeriod);
    List<PerformanceReview> findByReviewerEmailIgnoreCaseAndStatusNotOrderByCreatedAtDesc(
        String reviewerEmail, String status
    );
    List<PerformanceReview> findByStatusNotOrderByCreatedAtDesc(String status);
}