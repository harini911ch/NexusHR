package com.nexushr.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexushr.notification.dto.CreateNotificationRequest;
import com.nexushr.notification.service.NotificationDeliveryWorker;
import com.nexushr.notification.service.NotificationEmailSender;
import com.nexushr.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "management.health.mail.enabled=false",
    "app.notifications.internal-token=test-internal-token",
    "app.email.enabled=true",
    "app.email.delivery.poll-interval-ms=60000",
    "app.email.delivery.max-attempts=2"
})
@Import(NotificationDeliveryIntegrationTest.EmailSenderTestConfiguration.class)
class NotificationDeliveryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationDeliveryWorker deliveryWorker;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NotificationEmailSender emailSender;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", () ->
            POSTGRES.getJdbcUrl() + "&currentSchema=notification_data");
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetEmailSender() {
        reset(emailSender);
    }

    @Test
    void notificationIsPersistedBeforeEmailAndTransientFailuresAreRetried() {
        doThrow(new IllegalStateException("temporary SMTP failure"))
            .doNothing()
            .when(emailSender).send(any());

        var notification = createNotification("Retry notification");
        assertEquals(1, countFor(notification.id()));

        deliveryWorker.processDueDeliveries();
        assertEquals("PENDING", statusFor(notification.id()));
        assertEquals(1, attemptsFor(notification.id()));

        makeDeliveryDue(notification.id());
        deliveryWorker.processDueDeliveries();

        assertEquals("DELIVERED", statusFor(notification.id()));
        assertEquals(2, attemptsFor(notification.id()));
        verify(emailSender, times(2)).send(any());
    }

    @Test
    void exhaustedEmailDeliveryIsVisibleAndCanBeRetriedByAnAdministrator() {
        doThrow(new IllegalStateException("permanent SMTP failure"))
            .when(emailSender).send(any());

        var notification = createNotification("Failed notification");
        deliveryWorker.processDueDeliveries();
        makeDeliveryDue(notification.id());
        deliveryWorker.processDueDeliveries();

        assertEquals("DEAD", statusFor(notification.id()));
        var failures = notificationService.getDeliveryFailures();
        assertEquals(1, failures.size());
        assertEquals("permanent SMTP failure", failures.getFirst().lastError());

        notificationService.retryDeliveryFailure(failures.getFirst().id());
        assertEquals("PENDING", statusFor(notification.id()));
        assertEquals(0, attemptsFor(notification.id()));
        assertFalse(notificationService.getDeliveryFailures().stream()
            .anyMatch(failure -> failure.id().equals(failures.getFirst().id())));
    }

    @Test
    void duplicateProducerEventIsAcceptedWithoutDuplicatingNotificationOrDelivery() {
        var request = new CreateNotificationRequest(
            "employee@example.test", "Payroll approved", "Your payroll is ready",
            "PAYROLL", "PAYROLL_RUN", "run-123"
        );
        var eventId = java.util.UUID.randomUUID();

        notificationService.createInternal(request, "test-internal-token", eventId);
        notificationService.createInternal(request, "test-internal-token", eventId);

        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notifications WHERE source_event_id = ?",
            Integer.class, eventId
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM notification_delivery_outbox delivery
            JOIN notifications notification ON notification.id = delivery.notification_id
            WHERE notification.source_event_id = ?
            """,
            Integer.class, eventId
        ));
    }

    private com.nexushr.notification.dto.NotificationResponse createNotification(String title) {
        return notificationService.create(new CreateNotificationRequest(
            "employee@example.test", title, "A durable notification", "GENERAL", null, null
        ));
    }

    private int countFor(java.util.UUID notificationId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notification_delivery_outbox WHERE notification_id = ?",
            Integer.class, notificationId
        );
    }

    private String statusFor(java.util.UUID notificationId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM notification_delivery_outbox WHERE notification_id = ?",
            String.class, notificationId
        );
    }

    private int attemptsFor(java.util.UUID notificationId) {
        return jdbcTemplate.queryForObject(
            "SELECT attempt_count FROM notification_delivery_outbox WHERE notification_id = ?",
            Integer.class, notificationId
        );
    }

    private void makeDeliveryDue(java.util.UUID notificationId) {
        jdbcTemplate.update("""
            UPDATE notification_delivery_outbox
            SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
            WHERE notification_id = ?
            """, notificationId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailSenderTestConfiguration {
        @Bean
        @Primary
        NotificationEmailSender testEmailSender() {
            return org.mockito.Mockito.mock(NotificationEmailSender.class);
        }
    }
}
