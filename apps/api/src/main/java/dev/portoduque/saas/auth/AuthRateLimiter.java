package dev.portoduque.saas.auth;

import dev.portoduque.saas.shared.api.ApiProblemException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class AuthRateLimiter {

    private static final String RECORD_ATTEMPT_SQL = """
            INSERT INTO auth_rate_limits (action, subject_hash, window_started_at, attempts)
            VALUES (?, ?, ?, 1)
            ON CONFLICT (action, subject_hash) DO UPDATE SET
                window_started_at = CASE
                    WHEN auth_rate_limits.window_started_at < EXCLUDED.window_started_at
                    THEN EXCLUDED.window_started_at
                    ELSE auth_rate_limits.window_started_at
                END,
                attempts = CASE
                    WHEN auth_rate_limits.window_started_at < EXCLUDED.window_started_at
                    THEN 1
                    ELSE auth_rate_limits.attempts + 1
                END
            RETURNING attempts
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    AuthRateLimiter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void check(AuthAction action, String subject, int maximumAttempts, Duration window) {
        Instant now = clock.instant();
        long windowSeconds = window.toSeconds();
        Instant windowStart = Instant.ofEpochSecond((now.getEpochSecond() / windowSeconds) * windowSeconds);
        Integer attempts = jdbcTemplate.queryForObject(
                RECORD_ATTEMPT_SQL,
                Integer.class,
                action.name(),
                SecretHashing.sha256(subject),
                Timestamp.from(windowStart));
        if (attempts != null && attempts > maximumAttempts) {
            throw new ApiProblemException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED",
                    "Too many requests",
                    "Too many attempts. Try again later.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void clear(AuthAction action, String subject) {
        jdbcTemplate.update(
                "DELETE FROM auth_rate_limits WHERE action = ? AND subject_hash = ?",
                action.name(),
                SecretHashing.sha256(subject));
    }
}
