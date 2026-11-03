package com.nexushr.notification.controller;

import com.nexushr.notification.dto.CreateNotificationRequest;
import com.nexushr.notification.dto.DeliveryFailureResponse;
import com.nexushr.notification.dto.NotificationResponse;
import com.nexushr.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')")
    public NotificationResponse create(
        @Valid @RequestBody CreateNotificationRequest request
    ) {
        return service.create(request);
    }

    @PostMapping("/internal/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse createInternal(
        @Valid @RequestBody CreateNotificationRequest request,
        @RequestHeader(name = "X-Internal-Token", required = false) String internalToken,
        @RequestHeader(name = "X-Notification-Event-Id") UUID sourceEventId
    ) {
        return service.createInternal(request, internalToken, sourceEventId);
    }

    @GetMapping("/admin/delivery-failures")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    public List<DeliveryFailureResponse> deliveryFailures() {
        return service.getDeliveryFailures();
    }

    @PutMapping("/admin/delivery-failures/{id}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    public void retryDeliveryFailure(@PathVariable UUID id) {
        service.retryDeliveryFailure(id);
    }

    @GetMapping("/me")
    public List<NotificationResponse> myNotifications(Authentication auth) {
        return service.getMyNotifications(auth.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("unreadCount", service.getUnreadCount(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(
        @PathVariable UUID id,
        Authentication auth
    ) {
        return service.markAsRead(id, auth.getName());
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllAsRead(Authentication auth) {
        return Map.of("updatedCount", service.markAllAsRead(auth.getName()));
    }
}
