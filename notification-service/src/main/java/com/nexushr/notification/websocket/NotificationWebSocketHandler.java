package com.nexushr.notification.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushr.notification.dto.NotificationResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger =
        LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions =
        new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Principal principal = session.getPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        sessions.computeIfAbsent(normalize(principal.getName()),
            ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Principal principal = session.getPrincipal();
        if (principal == null) return;
        String email = normalize(principal.getName());
        Set<WebSocketSession> userSessions = sessions.get(email);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) sessions.remove(email, userSessions);
        }
    }

    public void publish(NotificationResponse notification) {
        Set<WebSocketSession> userSessions =
            sessions.get(normalize(notification.recipientEmail()));
        if (userSessions == null || userSessions.isEmpty()) return;

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException exception) {
            logger.error("Unable to serialize an in-app notification for WebSocket delivery", exception);
            return;
        }

        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                userSessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    if (session.isOpen()) session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException exception) {
                logger.warn("WebSocket notification delivery failed for an active session", exception);
            }
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
