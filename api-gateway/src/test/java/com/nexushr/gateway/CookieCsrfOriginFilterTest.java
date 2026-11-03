package com.nexushr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CookieCsrfOriginFilterTest {

    private final CookieCsrfOriginFilter filter = new CookieCsrfOriginFilter(
        "https://app.example.test,http://localhost:3000"
    );

    @Test
    void allowsCookieMutationFromConfiguredOrigin() {
        var exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/employees")
                .header(HttpHeaders.ORIGIN, "https://app.example.test")
                .cookie(new HttpCookie("nexushr_access", "opaque"))
                .build()
        );
        var chained = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chained.get());
    }

    @Test
    void rejectsCookieMutationFromUnconfiguredOrigin() {
        var exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/employees")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .cookie(new HttpCookie("nexushr_access", "opaque"))
                .build()
        );
        var chained = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsCookieMutationWithoutOrigin() {
        var exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/auth/refresh")
                .cookie(new HttpCookie("nexushr_refresh", "opaque"))
                .build()
        );
        var chained = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void doesNotRequireOriginForBearerClients() {
        var exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/employees")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build()
        );
        var chained = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chained.get());
    }
}
