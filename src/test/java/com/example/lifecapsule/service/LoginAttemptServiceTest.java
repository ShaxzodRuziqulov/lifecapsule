package com.example.lifecapsule.service;

import com.example.lifecapsule.errors.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    });

    @Test
    void locksAfterRepeatedFailuresRegardlessOfCase() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) service.recordFailure("Member");
        assertThatThrownBy(() -> service.checkAllowed("member")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void unlocksOnceTheWindowPasses() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) service.recordFailure("member");
        now.set(now.get().plus(LoginAttemptService.WINDOW).plusSeconds(1));
        assertThatCode(() -> service.checkAllowed("member")).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginClearsEarlierFailures() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) service.recordFailure("member");
        service.recordSuccess("member");
        service.recordFailure("member");
        assertThatCode(() -> service.checkAllowed("member")).doesNotThrowAnyException();
    }
}
