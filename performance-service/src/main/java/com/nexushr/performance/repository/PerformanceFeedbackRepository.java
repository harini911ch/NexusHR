package com.nexushr.performance.repository;

import com.nexushr.performance.entity.PerformanceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PerformanceFeedbackRepository extends JpaRepository<PerformanceFeedback, UUID> {
    List<PerformanceFeedback> findByReviewIdOrderByCreatedAtDesc(UUID reviewId);
    boolean existsByReviewIdAndFromEmail(UUID reviewId, String email);
}