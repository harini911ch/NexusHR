package com.nexushr.common.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.notifications.outbox.enabled", havingValue = "true")
public class NotificationOutboxConfiguration {

    @Bean
    NotificationEventListener notificationEventListener(JdbcTemplate jdbcTemplate) {
        return new NotificationEventListener(jdbcTemplate);
    }

    @Bean
    NotificationOutboxRelay notificationOutboxRelay(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        RestClient.Builder restClientBuilder,
        @Value("${app.notifications.service-url:http://localhost:8087}")
        String notificationServiceUrl,
        @Value("${app.notifications.internal-token:}") String internalToken,
        @Value("${app.notifications.outbox.batch-size:20}") int batchSize,
        @Value("${app.notifications.outbox.max-attempts:12}") int maxAttempts,
        @Value("${app.notifications.outbox.lease-seconds:30}") int leaseSeconds
    ) {
        return new NotificationOutboxRelay(
            jdbcTemplate, transactionTemplate, restClientBuilder, notificationServiceUrl,
            internalToken, batchSize, maxAttempts, leaseSeconds
        );
    }
}
