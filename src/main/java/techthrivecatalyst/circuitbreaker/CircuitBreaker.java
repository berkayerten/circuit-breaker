package techthrivecatalyst.circuitbreaker;

import java.util.function.Supplier;

public interface CircuitBreaker<T> {

    enum State {
        OPEN,
        HALF_OPEN,
        CLOSED
    }

    State getState();

    T call(Supplier<T> supplier);

    int getFailureCount();
}
