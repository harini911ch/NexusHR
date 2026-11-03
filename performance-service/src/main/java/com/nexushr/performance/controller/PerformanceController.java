package com.nexushr.performance.controller;

import com.nexushr.performance.dto.*;
import com.nexushr.performance.service.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    private final PerformanceService service;

    public PerformanceController(PerformanceService service) {
        this.service = service;
    }

    @PostMapping("/goals")
    public GoalResponse createGoal(Authentication auth,
                                   @Valid @RequestBody GoalRequest request) {
        return GoalResponse.from(service.createGoal(auth.getName(), request));
    }

    @GetMapping("/goals/me")
    public List<GoalResponse> myGoals(Authentication auth) {
        return service.myGoals(auth.getName()).stream().map(GoalResponse::from).toList();
    }

    @PutMapping("/goals/{id}/progress")
    public GoalResponse updateGoal(Authentication auth, @PathVariable UUID id,
                                    @Valid @RequestBody GoalProgressRequest request) {
        return GoalResponse.from(service.updateGoal(auth.getName(), id, request));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','SUPER_ADMIN')")
    public ReviewResponse createReview(Authentication auth,
                                       @Valid @RequestBody ReviewCreateRequest request) {
        return ReviewResponse.from(
            service.createReview(request.employeeEmail(), auth.getName(), request)
        );
    }

    @GetMapping("/reviews/me")
    public List<ReviewResponse> myReviews(Authentication auth) {
        return service.myReviews(auth.getName()).stream().map(ReviewResponse::from).toList();
    }

    @GetMapping("/reviews/pending")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER','SUPER_ADMIN')")
    public List<ReviewResponse> pendingReviews(Authentication auth) {
        boolean hrAdmin = isHr(auth);
        return service.pendingReviews(auth.getName(), hrAdmin).stream()
            .map(ReviewResponse::from).toList();
    }

    @GetMapping("/reviews/{id}")
    public ReviewResponse getReview(Authentication auth, @PathVariable UUID id) {
        return ReviewResponse.from(service.getReview(id, auth.getName(), isHr(auth)));
    }

    @PostMapping("/reviews/{id}/feedback-invitations")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER','SUPER_ADMIN')")
    public FeedbackInvitationResponse inviteFeedback(
        Authentication auth,
        @PathVariable UUID id,
        @Valid @RequestBody FeedbackInvitationRequest request
    ) {
        return FeedbackInvitationResponse.from(
            service.inviteFeedback(auth.getName(), isHr(auth), id, request)
        );
    }

    @GetMapping("/feedback-invitations/me")
    public List<FeedbackInvitationResponse> myFeedbackInvitations(Authentication auth) {
        return service.myFeedbackInvitations(auth.getName()).stream()
            .map(FeedbackInvitationResponse::from).toList();
    }

    @PutMapping("/reviews/{id}/self-assessment")
    public ReviewResponse selfAssessment(Authentication auth, @PathVariable UUID id,
                                         @Valid @RequestBody SelfAssessmentRequest request) {
        return ReviewResponse.from(service.submitSelfAssessment(auth.getName(), id, request));
    }

    @PutMapping("/reviews/{id}/manager-assessment")
    @PreAuthorize("hasAnyRole('HR_ADMIN','HR_MANAGER','MANAGER','DEPARTMENT_MANAGER','SUPER_ADMIN')")
    public ReviewResponse managerAssessment(Authentication auth, @PathVariable UUID id,
                                            @Valid @RequestBody ManagerAssessmentRequest request) {
        return ReviewResponse.from(
            service.submitManagerAssessment(auth.getName(), isHr(auth), id, request)
        );
    }

    @PostMapping("/reviews/{id}/feedback")
    public FeedbackResponse submitFeedback(Authentication auth, @PathVariable UUID id,
                                           @Valid @RequestBody FeedbackRequest request) {
        return FeedbackResponse.from(service.submitFeedback(auth.getName(), id, request));
    }

    @GetMapping("/reviews/{id}/feedback")
    public List<FeedbackResponse> getFeedback(Authentication auth, @PathVariable UUID id) {
        return service.getFeedback(auth.getName(), isHr(auth), id)
            .stream().map(FeedbackResponse::from).toList();
    }

    @GetMapping("/scorecard/me")
    public Map<String, Object> myScorecard(Authentication auth) {
        return service.myScorecard(auth.getName());
    }

    private static boolean isHr(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority ->
            List.of("ROLE_HR_ADMIN", "ROLE_HR_MANAGER", "ROLE_SUPER_ADMIN")
                .contains(authority.getAuthority())
        );
    }
}