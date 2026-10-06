package com.example.lifecapsule.service;

import com.example.lifecapsule.errors.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after MAX_FAILURES wrong passwords for one username within
 * WINDOW, that username is locked for WINDOW. In-memory, so it resets on restart - enough for
 * a single-instance family app.
 */
@Service
public class LoginAttemptService {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String username) {
        Attempts current = attempts.get(key(username));
        if (current != null && current.lockedUntil != null && clock.instant().isBefore(current.lockedUntil)) {
            throw new TooManyRequestsException("Juda ko'p noto'g'ri urinish. 15 daqiqadan keyin qayta urinib ko'ring");
        }
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        attempts.compute(key(username), (ignored, current) -> {
            Attempts next = current == null || now.isAfter(current.windowStart.plus(WINDOW))
                    ? new Attempts(now, 0, null)
                    : current;
            int failures = next.failures + 1;
            return new Attempts(next.windowStart, failures, failures >= MAX_FAILURES ? now.plus(WINDOW) : null);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempts(Instant windowStart, int failures, Instant lockedUntil) {
    }
}
