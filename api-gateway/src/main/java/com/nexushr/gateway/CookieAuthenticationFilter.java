package com.nexushr.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CookieAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_COOKIE = "nexushr_access";
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();
        var accessCookie = request.getCookies().getFirst(ACCESS_COOKIE);
        String cookieToken = accessCookie == null ? null : accessCookie.getValue();

        if (path.startsWith(AUTH_PATH_PREFIX)) {
            if (cookieToken == null || cookieToken.isBlank()) {
                return chain.filter(exchange);
            }

            var authRequest = request.mutate().headers(headers -> {
                if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    headers.setBearerAuth(cookieToken);
                }
            }).build();
            return chain.filter(exchange.mutate().request(authRequest).build());
        }

        var updatedRequest = request.mutate().headers(headers -> {
            headers.remove(HttpHeaders.COOKIE);
            if (cookieToken != null && !cookieToken.isBlank() &&
                !headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                headers.setBearerAuth(cookieToken);
            }
        }).build();

        return chain.filter(exchange.mutate().request(updatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
