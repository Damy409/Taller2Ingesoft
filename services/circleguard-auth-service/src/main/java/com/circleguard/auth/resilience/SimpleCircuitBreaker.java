package com.circleguard.auth.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

@Component
public class SimpleCircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private int failures;
    private State state = State.CLOSED;
    private Instant openedAt = Instant.MIN;

    public SimpleCircuitBreaker(
            @Value("${circleguard.resilience.identity.failure-threshold:3}") int failureThreshold,
            @Value("${circleguard.resilience.identity.open-duration-ms:30000}") long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDuration = Duration.ofMillis(openDurationMs);
    }

    public synchronized <T> T execute(Supplier<T> primary, Supplier<T> fallback) {
        if (state == State.OPEN && openedAt.plus(openDuration).isAfter(Instant.now())) {
            return fallback.get();
        }
        if (state == State.OPEN) {
            state = State.HALF_OPEN;
        }

        try {
            T result = primary.get();
            failures = 0;
            state = State.CLOSED;
            return result;
        } catch (RuntimeException ex) {
            failures++;
            if (failures >= failureThreshold) {
                state = State.OPEN;
                openedAt = Instant.now();
            }
            return fallback.get();
        }
    }
}

