package com.nexushr.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.nexushr.auth.service.PasswordResetRateLimiter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.health.mail.enabled=false"
)
class AuthPostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private Flyway flyway;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PasswordResetRateLimiter passwordResetRateLimiter;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET", () ->
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @Test
    void postgresMigrationsApplyAndAuthApplicationStarts() {
        flyway.validate();
        assertNotNull(flyway.info().current());
        assertEquals("3", flyway.info().current().getVersion().getVersion());
        assertEquals(HttpStatus.OK,
            restTemplate.getForEntity("/actuator/health", String.class).getStatusCode());
    }

    @Test
    void passwordResetRateLimiterEnforcesAndExpiresItsWindow() {
        String email = UUID.randomUUID() + "@example.test";
        Instant now = Instant.now();

        assertTrue(passwordResetRateLimiter.allowRequest(email, now));
        assertTrue(passwordResetRateLimiter.allowRequest(email, now.plusSeconds(1)));
        assertTrue(passwordResetRateLimiter.allowRequest(email, now.plusSeconds(2)));
        assertFalse(passwordResetRateLimiter.allowRequest(email, now.plusSeconds(3)));
        assertTrue(passwordResetRateLimiter.allowRequest(
            email, now.plus(Duration.ofHours(1)).plusSeconds(1)
        ));
    }
}
