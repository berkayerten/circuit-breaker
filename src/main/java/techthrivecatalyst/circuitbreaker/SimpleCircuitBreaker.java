package techthrivecatalyst.circuitbreaker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static techthrivecatalyst.circuitbreaker.CircuitBreaker.State.CLOSED;
import static techthrivecatalyst.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static techthrivecatalyst.circuitbreaker.CircuitBreaker.State.OPEN;

public class SimpleCircuitBreaker<T> implements CircuitBreaker<T> {


    private final int failureThreshold;
    private final int minSuccessThreshold;
    private final Duration openDuration;
    private final AtomicReference<State> currentState;
    private final Supplier<T> fallback;

    private final AtomicInteger resetOnSuccessCount = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant lastFailureTime = Instant.MIN;

    public SimpleCircuitBreaker(int failureThreshold, int minSuccessThreshold, Duration openDuration,
            Supplier<T> fallback) {
        this.failureThreshold = failureThreshold;
        this.minSuccessThreshold = minSuccessThreshold;
        this.openDuration = openDuration;
        this.currentState = new AtomicReference<>(CLOSED);
        this.fallback = fallback;
    }

    @Override
    public State getState() {
        return currentState.get();
    }

    @Override
    public T call(Supplier<T> supplier) {
        State current = currentState.get();
        if (current == OPEN && isCooldownOver()) {
            synchronized (this) {
                if (currentState.get() == OPEN && isCooldownOver()) {
                    this.currentState.set(HALF_OPEN);
                    consecutiveFailures.set(0);
                    return getFallbackValue();
                }
            }
        }

        current = currentState.get();

        if (current == OPEN) {
            return getFallbackValue();
        } else if (current == HALF_OPEN && consecutiveFailures.incrementAndGet() > failureThreshold) {
            return getFallbackValue();
        }

        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    @Override
    public int getFailureCount() {
        return consecutiveFailures.get();
    }

    private boolean isCooldownOver() {
        return Duration.between(lastFailureTime, Instant.now()).compareTo(openDuration) > 0;
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        if (this.currentState.get() == CLOSED) {
            synchronized (this) {
                if (resetOnSuccessCount.get() >= minSuccessThreshold) {
                    this.currentState.set(CLOSED);
                    resetOnSuccessCount.set(0);
                } else {
                    resetOnSuccessCount.incrementAndGet();
                }
            }
        }
    }

    private void onFailure() {
        this.lastFailureTime = Instant.now();
        synchronized (this) {
            if (this.currentState.get() == CLOSED && consecutiveFailures.incrementAndGet() >= failureThreshold) {
                this.currentState.set(OPEN);
            }
        }
    }

    private T getFallbackValue() {
        try {
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            throw new FallbackExecutionException("Fallback execution failed.");
        }
    }
}
