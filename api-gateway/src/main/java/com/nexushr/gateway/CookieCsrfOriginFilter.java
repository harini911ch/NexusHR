package com.nexushr.gateway;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CookieCsrfOriginFilter implements GlobalFilter, Ordered {

    private static final Set<HttpMethod> SAFE_METHODS =
        Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);
    private static final String ACCESS_COOKIE = "nexushr_access";
    private static final String REFRESH_COOKIE = "nexushr_refresh";

    private final Set<String> allowedOrigins;

    public CookieCsrfOriginFilter(
        @Value("${app.security.allowed-origins}") String configuredOrigins
    ) {
        this.allowedOrigins = Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        if (SAFE_METHODS.contains(request.getMethod()) ||
            (!request.getCookies().containsKey(ACCESS_COOKIE) &&
             !request.getCookies().containsKey(REFRESH_COOKIE))) {
            return chain.filter(exchange);
        }

        String origin = request.getHeaders().getOrigin();
        if (origin != null && allowedOrigins.contains(origin)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
