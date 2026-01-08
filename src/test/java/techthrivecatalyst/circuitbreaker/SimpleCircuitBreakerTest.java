package techthrivecatalyst.circuitbreaker;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleCircuitBreakerTest {

    @Test
    void shouldCallWithSuccess() {
        CircuitBreaker<String> circuitBreaker = new SimpleCircuitBreaker<>(3, 2, Duration.ofMillis(500), null);
        String result = executeCall(circuitBreaker, true);

        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals("200", result);
    }

    @Test
    void shouldOpenCircuitAfterManyFailedAttempts() {
        CircuitBreaker<String> circuitBreaker = new SimpleCircuitBreaker<>(3, 2, Duration.ofMillis(500), null);
        for (int i = 0; i < 3; i++) {
            try {
                executeCall(circuitBreaker, false);
            } catch (IllegalStateException e) {
                System.out.println("call failed");
            }
        }

        assertEquals(3, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void shouldSwitchToHalfOpenStateAfterOpenDurationExceeds() throws InterruptedException {
        CircuitBreaker<String> circuitBreaker = new SimpleCircuitBreaker<>(3, 2, Duration.ofMillis(500), null);
        for (int i = 0; i < 3; i++) {
            try {
                executeCall(circuitBreaker, false);
            } catch (IllegalStateException e) {
                System.out.println("call failed");
            }
        }

        Thread.sleep(1000);
        executeCall(circuitBreaker, true);
        assertEquals(0, circuitBreaker.getFailureCount()); // reset after half open
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    private String executeCall(CircuitBreaker<String> circuitBreaker, boolean shouldSucceed) {
        return circuitBreaker.call(() -> {
            try {
                return externalCall(shouldSucceed);
            } catch (IOException e) {
                System.out.println("IO Exception");
            } catch (InterruptedException e) {
                System.out.println("Interrupted Exception");
            }
            return "failed";
        });
    }

    private String externalCall(boolean shouldSucceed) throws IOException, InterruptedException {
        if (shouldSucceed) {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return String.valueOf(response.statusCode());
        } else {
            Thread.sleep(200); // long running call
            throw new IllegalStateException("failed");
        }
    }

}