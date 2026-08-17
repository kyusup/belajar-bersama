package id.belajarbersama.domain.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    @Test
    void allowsUpToLimitThenDeniesInsideTheSameWindow() {
        RateLimiter limiter = new RateLimiter(Duration.ofMinutes(1));
        Instant now = Instant.parse("2026-08-18T00:00:10Z");
        RateLimiter.Result last = RateLimiter.Result.allow();
        for (int index = 0; index < 3; index++) {
            last = limiter.allow("user-1", RateLimiter.Bucket.WRITE, 3, now);
            assertTrue(last.allowed());
        }
        last = limiter.allow("user-1", RateLimiter.Bucket.WRITE, 3, now);
        assertFalse(last.allowed());
        assertTrue(last.retryAfterSeconds() >= 1);
        assertTrue(
                limiter.allow("user-2", RateLimiter.Bucket.WRITE, 3, now).allowed(),
                "A different identity has its own budget.");
        assertTrue(
                limiter.allow("user-1", RateLimiter.Bucket.AUTH, 3, now).allowed(),
                "A different bucket has its own budget.");
    }

    @Test
    void resetsAfterTheWindow() {
        RateLimiter limiter = new RateLimiter(Duration.ofSeconds(1));
        Instant first = Instant.parse("2026-08-18T00:00:00.200Z");
        assertTrue(limiter.allow("ip", RateLimiter.Bucket.AUTH, 1, first).allowed());
        assertFalse(limiter.allow("ip", RateLimiter.Bucket.AUTH, 1, first).allowed());
        Instant nextWindow = Instant.parse("2026-08-18T00:00:01.050Z");
        assertTrue(limiter.allow("ip", RateLimiter.Bucket.AUTH, 1, nextWindow).allowed());
    }

    @Test
    void zeroLimitFailsClosed() {
        RateLimiter limiter = new RateLimiter();
        RateLimiter.Result result =
                limiter.allow(
                        "ip", RateLimiter.Bucket.REPORT, 0, Instant.parse("2026-08-18T00:00:00Z"));
        assertFalse(result.allowed());
        assertEquals(1, result.retryAfterSeconds());
    }
}
