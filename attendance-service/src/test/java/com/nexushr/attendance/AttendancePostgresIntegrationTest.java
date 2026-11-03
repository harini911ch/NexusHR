package com.nexushr.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nexushr.common.notification.NotificationEvent;
import com.nexushr.common.notification.NotificationOutboxRelay;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.flywaydb.core.Flyway;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttendancePostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine");
    private static final AtomicInteger notificationRequests = new AtomicInteger();
    private static final CopyOnWriteArrayList<String> eventIds = new CopyOnWriteArrayList<>();
    private static HttpServer notificationServer;

    @Autowired private Flyway flyway;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private NotificationOutboxRelay outboxRelay;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        startNotificationServer();
        registry.add("DB_URL", () -> POSTGRES.getJdbcUrl() + "&currentSchema=attendance_data");
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET", () ->
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("NOTIFICATION_OUTBOX_POLL_INTERVAL_MS", () -> "60000");
        registry.add("APP_NOTIFICATIONS_SERVICE_URL", () ->
            "http://127.0.0.1:" + notificationServer.getAddress().getPort());
        registry.add("APP_NOTIFICATIONS_INTERNAL_TOKEN", () -> "integration-test-token");
    }

    @BeforeEach
    void clearOutbox() {
        jdbcTemplate.update("DELETE FROM notification_outbox");
        notificationRequests.set(0);
        eventIds.clear();
    }

    @AfterAll
    static void stopNotificationServer() {
        if (notificationServer != null) notificationServer.stop(0);
    }

    @Test
    void postgresMigrationsApplyAndAttendanceApplicationStarts() {
        flyway.validate();
        assertNotNull(flyway.info().current());
        assertEquals("3", flyway.info().current().getVersion().getVersion());
        assertEquals(HttpStatus.OK,
            restTemplate.getForEntity("/actuator/health", String.class).getStatusCode());
    }

    @Test
    void notificationEventsAreWrittenOnlyWhenTheirBusinessTransactionCommits() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NotificationEvent(
                "employee@example.test", "Attendance recorded", "Check-in complete",
                "ATTENDANCE", "ATTENDANCE", "entry-1"
            ));
        });

        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notification_outbox WHERE related_entity_id = 'entry-1'",
            Integer.class
        ));

        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NotificationEvent(
                "employee@example.test", "Attendance failed", "This must roll back",
                "ATTENDANCE", "ATTENDANCE", "entry-2"
            ));
            throw new IllegalStateException("rollback");
        }));
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notification_outbox", Integer.class
        ));
    }

    @Test
    void outboxRelayRetriesAnUnavailableNotificationServiceWithoutLosingTheEvent() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> eventPublisher.publishEvent(new NotificationEvent(
            "employee@example.test", "Attendance recorded", "Check-in complete",
            "ATTENDANCE", "ATTENDANCE", "relay-entry"
        )));
        UUID eventId = jdbcTemplate.queryForObject(
            "SELECT id FROM notification_outbox WHERE related_entity_id = 'relay-entry'",
            UUID.class
        );

        outboxRelay.relayDueEvents();
        assertEquals("PENDING", statusFor(eventId));
        assertEquals(1, attemptsFor(eventId));

        jdbcTemplate.update("""
            UPDATE notification_outbox
            SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
            WHERE id = ?
            """, eventId);
        outboxRelay.relayDueEvents();

        assertEquals("DELIVERED", statusFor(eventId));
        assertEquals(2, attemptsFor(eventId));
        assertEquals(2, notificationRequests.get());
        assertEquals(2, eventIds.size());
        assertEquals(eventIds.getFirst(), eventIds.getLast());
    }

    private String statusFor(UUID id) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM notification_outbox WHERE id = ?", String.class, id
        );
    }

    private int attemptsFor(UUID id) {
        return jdbcTemplate.queryForObject(
            "SELECT attempt_count FROM notification_outbox WHERE id = ?", Integer.class, id
        );
    }

    private static void startNotificationServer() {
        try {
            notificationServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            notificationServer.createContext("/api/notifications/internal/events", exchange -> {
                eventIds.add(exchange.getRequestHeaders().getFirst("X-Notification-Event-Id"));
                exchange.getRequestBody().readAllBytes();
                int attempt = notificationRequests.incrementAndGet();
                int status = attempt == 1 ? 503 : 202;
                byte[] response = "accepted".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, response.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(response);
                }
            });
            notificationServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start notification outbox test endpoint",
                exception);
        }
    }
}
