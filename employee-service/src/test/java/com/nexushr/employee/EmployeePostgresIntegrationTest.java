package com.nexushr.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
class EmployeePostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private Flyway flyway;
    @Autowired private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", () -> POSTGRES.getJdbcUrl() + "&currentSchema=employee_data");
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET", () ->
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("EMPLOYEE_DOCUMENTS_DIRECTORY", () ->
            System.getProperty("java.io.tmpdir") + "/nexushr-test-documents");
    }

    @Test
    void postgresMigrationsApplyAndEmployeeApplicationStarts() {
        flyway.validate();
        assertNotNull(flyway.info().current());
        assertEquals("5", flyway.info().current().getVersion().getVersion());
        assertEquals(HttpStatus.OK,
            restTemplate.getForEntity("/actuator/health", String.class).getStatusCode());
    }

    @Test
    void unauthenticatedEmployeeRequestReturnsUnauthorizedForSessionRefresh() {
        assertEquals(HttpStatus.UNAUTHORIZED,
            restTemplate.getForEntity("/api/employees/me", String.class).getStatusCode());
    }
}
