package com.nexushr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

class CookieAuthenticationFilterTest {

    private final CookieAuthenticationFilter filter = new CookieAuthenticationFilter();

    @Test
    void forwardsAccessCookieAsBearerAndStripsAllCookiesForBusinessServices() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
                .cookie(
                    new HttpCookie("nexushr_access", "access-value"),
                    new HttpCookie("nexushr_refresh", "refresh-value")
                )
                .build()
        );
        var observed = new ObservedRequest();

        filter.filter(exchange, captureRequest(observed)).block();

        assertEquals("Bearer access-value", observed.authorization);
        assertNull(observed.cookieHeader);
    }

    @Test
    void preservesRefreshCookieAndForwardsAccessForCurrentUserEndpoint() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/auth/me")
                .cookie(
                    new HttpCookie("nexushr_access", "access-value"),
                    new HttpCookie("nexushr_refresh", "refresh-value")
                )
                .build()
        );
        var observed = new ObservedRequest();

        filter.filter(exchange, captureRequest(observed)).block();

        assertEquals("Bearer access-value", observed.authorization);
        assertEquals(Set.of("nexushr_access", "nexushr_refresh"), observed.cookieNames);
    }

    @Test
    void forwardsAccessCookieForInvitationEndpoint() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.method(HttpMethod.POST, "/api/auth/invitations")
                .cookie(new HttpCookie("nexushr_access", "access-value"))
                .build()
        );
        var observed = new ObservedRequest();

        filter.filter(exchange, captureRequest(observed)).block();

        assertNotNull(observed.authorization);
        assertTrue(observed.authorization.startsWith("Bearer "));
        assertEquals(Set.of("nexushr_access"), observed.cookieNames);
    }

    @Test
    void stripsRefreshCookieWhenNoAccessCookieExists() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/employees")
                .cookie(new HttpCookie("nexushr_refresh", "refresh-value"))
                .build()
        );
        var observed = new ObservedRequest();

        filter.filter(exchange, captureRequest(observed)).block();

        assertNull(observed.authorization);
        assertNull(observed.cookieHeader);
    }

    private static org.springframework.cloud.gateway.filter.GatewayFilterChain
    captureRequest(ObservedRequest observed) {
        return exchange -> {
            observed.authorization =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            observed.cookieHeader =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
            observed.cookieNames = exchange.getRequest().getCookies().keySet();
            return Mono.empty();
        };
    }

    private static final class ObservedRequest {
        private String authorization;
        private String cookieHeader;
        private Set<String> cookieNames;
    }
}
