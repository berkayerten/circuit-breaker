package techthrivecatalyst.circuitbreaker;

public class FallbackExecutionException extends RuntimeException {

    public FallbackExecutionException(String msg) {
        super(msg);
    }
}
