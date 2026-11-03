package com.nexushr.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nexushr.insights.dto.AttritionRequest;
import com.nexushr.insights.service.InsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
class AiProviderStartupIntegrationTest {

    @Autowired private InsightsService insightsService;
    @Autowired private TestRestTemplate restTemplate;

    @Test
    void applicationStartsWithoutProviderCredentialsAndUsesDeterministicFallback() {
        var result = insightsService.attrition(
            new AttritionRequest("demo@example.com", 70.0, 30.0, 2.0, 30, 7, 30)
        );

        assertEquals("DETERMINISTIC_FALLBACK", result.get("recommendationSource"));
        assertEquals(HttpStatus.OK,
            restTemplate.getForEntity("/actuator/health", String.class).getStatusCode());
    }
}
