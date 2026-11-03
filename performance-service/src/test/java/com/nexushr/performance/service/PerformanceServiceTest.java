package com.nexushr.performance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.performance.dto.FeedbackRequest;
import com.nexushr.performance.entity.PerformanceFeedback;
import com.nexushr.performance.entity.PerformanceFeedbackInvitation;
import com.nexushr.performance.entity.PerformanceReview;
import com.nexushr.performance.repository.PerformanceFeedbackInvitationRepository;
import com.nexushr.performance.repository.PerformanceFeedbackRepository;
import com.nexushr.performance.repository.PerformanceGoalRepository;
import com.nexushr.performance.repository.PerformanceReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock private PerformanceGoalRepository goalRepository;
    @Mock private PerformanceReviewRepository reviewRepository;
    @Mock private PerformanceFeedbackRepository feedbackRepository;
    @Mock private PerformanceFeedbackInvitationRepository invitationRepository;

    private PerformanceService service;

    @BeforeEach
    void setUp() {
        service = new PerformanceService(
            goalRepository, reviewRepository, feedbackRepository, invitationRepository
        );
    }

    @Test
    void feedbackRequiresInvitationAndUsesInvitedRelationship() {
        UUID reviewId = UUID.randomUUID();
        PerformanceReview review =
            new PerformanceReview("employee@example.com", "manager@example.com", "FY2026");
        ReflectionTestUtils.setField(review, "id", reviewId);
        PerformanceFeedbackInvitation invitation = new PerformanceFeedbackInvitation(
            reviewId, "peer@example.com", "PEER", "manager@example.com"
        );
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(invitationRepository.findForUpdate(reviewId, "peer@example.com"))
            .thenReturn(Optional.of(invitation));
        when(feedbackRepository.save(any(PerformanceFeedback.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.save(invitation)).thenReturn(invitation);

        PerformanceFeedback feedback = service.submitFeedback(
            "Peer@Example.com", reviewId, new FeedbackRequest(4, "Great collaboration")
        );

        assertEquals("PEER", feedback.getRelationship());
        assertEquals("peer@example.com", feedback.getFromEmail());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void uninvitedEmployeeCannotSubmitFeedback() {
        UUID reviewId = UUID.randomUUID();
        PerformanceReview review =
            new PerformanceReview("employee@example.com", "manager@example.com", "FY2026");
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(invitationRepository.findForUpdate(reviewId, "peer@example.com"))
            .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.submitFeedback(
            "peer@example.com", reviewId, new FeedbackRequest(4, "Feedback")
        ));
    }
}
