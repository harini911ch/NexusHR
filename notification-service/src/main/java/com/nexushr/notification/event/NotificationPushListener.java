package com.nexushr.notification.event;

import com.nexushr.notification.websocket.NotificationWebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationPushListener {

    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationPushListener(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationPushEvent event) {
        webSocketHandler.publish(event.notification());
    }
}
