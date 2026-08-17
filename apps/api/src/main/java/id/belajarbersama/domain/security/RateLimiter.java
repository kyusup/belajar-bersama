package id.belajarbersama.domain.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixed-window limiter. In-memory and per process — suitable for a single API instance. */
public final class RateLimiter {
    public enum Bucket {
        AUTH,
        WRITE,
        REPORT,
        SEARCH,
        PUBLIC
    }

    public record Result(boolean allowed, long retryAfterSeconds) {
        public static Result allow() {
            return new Result(true, 0);
        }

        public static Result deny(long retryAfterSeconds) {
            return new Result(false, Math.max(1, retryAfterSeconds));
        }
    }

    private record Window(long startMs, AtomicInteger count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final long windowMs;

    public RateLimiter(Duration window) {
        this.windowMs = window == null ? 60_000L : Math.max(1_000L, window.toMillis());
    }

    public RateLimiter() {
        this(Duration.ofMinutes(1));
    }

    public Result allow(String identity, Bucket bucket, int limit, Instant now) {
        if (limit <= 0) {
            return Result.deny(1);
        }
        Instant at = now == null ? Instant.now() : now;
        String key =
                bucket.name()
                        + ":"
                        + (identity == null || identity.isBlank() ? "unknown" : identity);
        long nowMs = at.toEpochMilli();
        long start = nowMs - (nowMs % windowMs);
        Window window =
                windows.compute(
                        key,
                        (ignored, existing) -> {
                            if (existing == null || existing.startMs() != start) {
                                return new Window(start, new AtomicInteger(0));
                            }
                            return existing;
                        });
        int count = window.count().incrementAndGet();
        maybeSweep(nowMs);
        if (count > limit) {
            long retryMs = start + windowMs - nowMs;
            return Result.deny((retryMs + 999) / 1000);
        }
        return Result.allow();
    }

    int size() {
        return windows.size();
    }

    private void maybeSweep(long nowMs) {
        if (windows.size() < 2_000) {
            return;
        }
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next().getValue();
            if (window.startMs() + windowMs < nowMs) {
                iterator.remove();
            }
        }
    }
}
