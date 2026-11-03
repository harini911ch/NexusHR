package com.nexushr.performance.service;

import com.nexushr.performance.dto.*;
import com.nexushr.performance.entity.*;
import com.nexushr.performance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class PerformanceService {
    private final PerformanceGoalRepository goalRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final PerformanceFeedbackRepository feedbackRepository;
    private final PerformanceFeedbackInvitationRepository invitationRepository;

    public PerformanceService(
        PerformanceGoalRepository goalRepository,
        PerformanceReviewRepository reviewRepository,
        PerformanceFeedbackRepository feedbackRepository,
        PerformanceFeedbackInvitationRepository invitationRepository
    ) {
        this.goalRepository = goalRepository;
        this.reviewRepository = reviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    public PerformanceGoal createGoal(String email, GoalRequest request) {
        return goalRepository.save(new PerformanceGoal(
            email.toLowerCase(), request.title(), request.description(),
            request.category(), request.targetDate()
        ));
    }

    public List<PerformanceGoal> myGoals(String email) {
        return goalRepository.findByEmployeeEmailOrderByCreatedAtDesc(email.toLowerCase());
    }

    @Transactional
    public PerformanceGoal updateGoal(String email, UUID id, GoalProgressRequest request) {
        PerformanceGoal goal = goalRepository.findByIdAndEmployeeEmail(id, email.toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));

        String status = request.status().toUpperCase();
        if (!Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "ON_HOLD").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid goal status");
        }
        if (request.progress() == 100 && !"COMPLETED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A goal at 100% progress must have COMPLETED status");
        }
        if ("COMPLETED".equals(status) && request.progress() != 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A completed goal must have 100% progress");
        }
        goal.updateProgress(request.progress(), status);
        return goal;
    }

    @Transactional
    public PerformanceReview createReview(String employeeEmail, String reviewerEmail,
                                          ReviewCreateRequest request) {
        String employee = employeeEmail.toLowerCase();
        if (employee.equalsIgnoreCase(reviewerEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A reviewer cannot create a review for themselves");
        }
        if (reviewRepository.existsByEmployeeEmailAndReviewPeriod(employee, request.reviewPeriod())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A review already exists for this employee and period");
        }
        return reviewRepository.save(new PerformanceReview(
            employee, reviewerEmail.toLowerCase(), request.reviewPeriod()
        ));
    }

    public List<PerformanceReview> myReviews(String email) {
        return reviewRepository.findByEmployeeEmailOrderByCreatedAtDesc(email.toLowerCase());
    }

    public List<PerformanceReview> pendingReviews(String email, boolean hrAdmin) {
        return hrAdmin
            ? reviewRepository.findByStatusNotOrderByCreatedAtDesc("COMPLETED")
            : reviewRepository.findByReviewerEmailIgnoreCaseAndStatusNotOrderByCreatedAtDesc(
                email, "COMPLETED"
            );
    }

    public PerformanceReview getReview(UUID id, String requester, boolean hrAdmin) {
        PerformanceReview review = reviewRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Review not found"));
        if (!hrAdmin && !review.getEmployeeEmail().equalsIgnoreCase(requester)
            && !review.getReviewerEmail().equalsIgnoreCase(requester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Review access denied");
        }
        return review;
    }

    @Transactional
    public PerformanceFeedbackInvitation inviteFeedback(
        String requester, boolean hrAdmin, UUID reviewId, FeedbackInvitationRequest request
    ) {
        PerformanceReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Review not found"));
        if (!hrAdmin && !review.getReviewerEmail().equalsIgnoreCase(requester)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only the assigned reviewer or HR can invite feedback");
        }
        if ("COMPLETED".equals(review.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Feedback cannot be invited for a completed review");
        }
        String invitee = normalizeEmail(request.inviteeEmail());
        if (invitee.equalsIgnoreCase(review.getEmployeeEmail()) ||
            invitee.equalsIgnoreCase(review.getReviewerEmail())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "The review subject and assigned reviewer cannot be 360 reviewers");
        }
        String relationship = request.relationship().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PEER", "MANAGER", "DIRECT_REPORT", "COLLEAGUE").contains(relationship)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid feedback relationship");
        }
        if (invitationRepository.existsByReviewIdAndInviteeEmailIgnoreCase(reviewId, invitee)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "This employee has already been invited to the review");
        }
        return invitationRepository.save(new PerformanceFeedbackInvitation(
            reviewId, invitee, relationship, normalizeEmail(requester)
        ));
    }

    public List<PerformanceFeedbackInvitation> myFeedbackInvitations(String email) {
        return invitationRepository
            .findByInviteeEmailIgnoreCaseAndSubmittedAtIsNullOrderByCreatedAtDesc(
                normalizeEmail(email)
            );
    }

    @Transactional
    public PerformanceReview submitSelfAssessment(String email, UUID reviewId,
                                                   SelfAssessmentRequest request) {
        PerformanceReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!review.getEmployeeEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your review");
        }
        try {
            review.submitSelfAssessment(request.rating(), request.assessment());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return review;
    }

    @Transactional
    public PerformanceReview submitManagerAssessment(String reviewerEmail, boolean hrAdmin,
                                                       UUID reviewId,
                                                       ManagerAssessmentRequest request) {
        PerformanceReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!hrAdmin && !review.getReviewerEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the assigned reviewer or HR admin can complete this review");
        }
        if ("COMPLETED".equals(review.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is already completed");
        }
        review.submitManagerAssessment(
            request.rating(), request.strengths(),
            request.improvementAreas(), request.comments()
        );
        return review;
    }

    @Transactional
    public PerformanceFeedback submitFeedback(String email, UUID reviewId,
                                               FeedbackRequest request) {
        PerformanceReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (review.getEmployeeEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You cannot submit 360-degree feedback for your own review");
        }
        PerformanceFeedbackInvitation invitation = invitationRepository
            .findForUpdate(reviewId, normalizeEmail(email))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "An invitation is required to submit 360-degree feedback"));
        if (invitation.getSubmittedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This feedback invitation has already been used");
        }
        if ("COMPLETED".equals(review.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Feedback cannot be submitted for a completed review");
        }
        PerformanceFeedback feedback = feedbackRepository.save(new PerformanceFeedback(
            reviewId, normalizeEmail(email), invitation.getRelationship(),
            request.rating(), request.comments()
        ));
        invitation.markSubmitted();
        invitationRepository.save(invitation);
        return feedback;
    }

    public List<PerformanceFeedback> getFeedback(String requester, boolean hrAdmin,
                                                   UUID reviewId) {
        PerformanceReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!hrAdmin && !review.getReviewerEmail().equalsIgnoreCase(requester)
            && !review.getEmployeeEmail().equalsIgnoreCase(requester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view feedback");
        }
        return feedbackRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
    }

    public Map<String, Object> myScorecard(String email) {
        List<PerformanceGoal> goals = myGoals(email);
        List<PerformanceReview> reviews = myReviews(email);
        double average = reviews.stream()
            .map(PerformanceReview::getManagerRating)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average().orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeEmail", email);
        result.put("totalGoals", goals.size());
        result.put("completedGoals", goals.stream()
            .filter(g -> "COMPLETED".equals(g.getStatus())).count());
        result.put("totalReviews", reviews.size());
        result.put("completedReviews", reviews.stream()
            .filter(r -> "COMPLETED".equals(r.getStatus())).count());
        result.put("averageManagerRating", Math.round(average * 100.0) / 100.0);
        result.put("goals", goals.stream().map(GoalResponse::from).toList());
        result.put("reviews", reviews.stream().map(ReviewResponse::from).toList());
        return result;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}