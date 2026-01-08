package techthrivecatalyst.circuitbreaker;

public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String msg) {
        super(msg);
    }
}
