package id.belajarbersama.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID id,
        UserId userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt) {
    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
