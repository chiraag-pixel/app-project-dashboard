package com.skillgap.security;

import com.skillgap.config.AppProperties;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Very small in-memory failed-login throttle: after N failed attempts for the
 * same username, further attempts are blocked for a few minutes. Prevents
 * trivial brute force without extra infrastructure (single-instance only).
 */
@Service
public class LoginThrottleService {

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMillis;

    public LoginThrottleService(AppProperties properties) {
        this.maxAttempts = properties.getSecurity().getLoginMaxAttempts();
        this.lockMillis = properties.getSecurity().getLoginLockMinutes() * 60_000L;
    }

    public boolean isBlocked(String username) {
        AttemptState state = attempts.get(normalize(username));
        if (state == null) {
            return false;
        }
        if (state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now())) {
            return true;
        }
        if (state.lockedUntil != null && state.lockedUntil.isBefore(Instant.now())) {
            attempts.remove(normalize(username));
        }
        return false;
    }

    public void recordFailure(String username) {
        attempts.compute(normalize(username), (key, state) -> {
            AttemptState next = state == null ? new AttemptState() : state;
            next.count++;
            if (next.count >= maxAttempts) {
                next.lockedUntil = Instant.now().plusMillis(lockMillis);
            }
            return next;
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.toLowerCase().trim();
    }

    private static final class AttemptState {
        private int count;
        private Instant lockedUntil;
    }
}
