package com.nexushr.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final String UPSERT_LIMIT = """
        INSERT INTO auth_rate_limit_buckets
            (bucket_hash, window_started_at, request_count, updated_at)
        VALUES (?, ?, 1, ?)
        ON CONFLICT (bucket_hash) DO UPDATE SET
            window_started_at = CASE
                WHEN auth_rate_limit_buckets.window_started_at <= ? THEN ?
                ELSE auth_rate_limit_buckets.window_started_at
            END,
            request_count = CASE
                WHEN auth_rate_limit_buckets.window_started_at <= ? THEN 1
                ELSE auth_rate_limit_buckets.request_count + 1
            END,
            updated_at = ?
        RETURNING request_count
        """;

    private final JdbcTemplate jdbcTemplate;
    private final byte[] key;

    public PasswordResetRateLimiter(
        JdbcTemplate jdbcTemplate,
        @Value("${app.jwt.secret}") String secret
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean allowRequest(String normalizedEmail, Instant now) {
        jdbcTemplate.update(
            "DELETE FROM auth_rate_limit_buckets WHERE updated_at < ?",
            Timestamp.from(now.minus(Duration.ofDays(1)))
        );

        Instant windowStart = now.minus(WINDOW);
        Integer requestCount = jdbcTemplate.queryForObject(
            UPSERT_LIMIT,
            Integer.class,
            hashIdentifier(normalizedEmail),
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(windowStart),
            Timestamp.from(now),
            Timestamp.from(windowStart),
            Timestamp.from(now)
        );
        return requestCount != null && requestCount <= MAX_REQUESTS_PER_WINDOW;
    }

    private String hashIdentifier(String identifier) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(
                identifier.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
