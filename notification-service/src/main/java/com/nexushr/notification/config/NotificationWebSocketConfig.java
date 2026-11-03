package com.nexushr.notification.config;

import com.nexushr.notification.websocket.NotificationWebSocketHandler;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final String[] allowedOrigins;

    public NotificationWebSocketConfig(
        NotificationWebSocketHandler handler,
        @Value("${app.websocket.allowed-origins}") String origins
    ) {
        this.handler = handler;
        this.allowedOrigins = Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
            .setAllowedOrigins(allowedOrigins);
    }
}
