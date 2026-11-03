package com.nexushr.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nexushr.insights.dto.AttritionRequest;
import com.nexushr.insights.service.InsightsService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.ai.provider=openai",
    "spring.ai.model.chat=openai",
    "spring.ai.openai.api-key=test-only-key",
    "spring.ai.retry.max-attempts=1"
})
class OpenAiProviderIntegrationTest {

    private static final AtomicReference<String> requestBody = new AtomicReference<>();
    private static HttpServer providerServer;

    @Autowired private InsightsService insightsService;

    @DynamicPropertySource
    static void providerEndpoint(DynamicPropertyRegistry registry) {
        try {
            providerServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            providerServer.createContext("/v1/chat/completions", exchange -> {
                requestBody.set(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8
                ));
                byte[] response = """
                    {"id":"chatcmpl-test","object":"chat.completion","created":1,
                     "model":"gpt-4o-mini","choices":[{"index":0,
                     "message":{"role":"assistant","content":"Offer a supportive career check-in"},
                     "finish_reason":"stop"}],
                     "usage":{"prompt_tokens":10,"completion_tokens":8,"total_tokens":18}}
                    """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(response);
                }
            });
            providerServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start the local Spring AI test provider",
                exception);
        }
        registry.add("spring.ai.openai.base-url", () ->
            "http://127.0.0.1:" + providerServer.getAddress().getPort());
    }

    @AfterAll
    static void stopProviderServer() {
        if (providerServer != null) providerServer.stop(0);
    }

    @Test
    void configuredSpringAiProviderReturnsRecommendationsWithoutSendingEmployeeEmail() {
        var result = insightsService.attrition(
            new AttritionRequest("private.employee@example.com", 70.0, 30.0, 2.0, 30, 7, 30)
        );

        assertEquals("SPRING_AI_OPENAI", result.get("recommendationSource"));
        assertEquals(
            java.util.List.of("Offer a supportive career check-in"),
            result.get("recommendations")
        );
        assertTrue(requestBody.get().contains("attendanceRate=70.0"));
        assertFalse(requestBody.get().contains("private.employee@example.com"));
    }
}
