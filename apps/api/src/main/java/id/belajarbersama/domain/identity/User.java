package id.belajarbersama.domain.identity;

import java.time.Instant;

/**
 * Application user. Must not be confused with an OAuth/OIDC subject.
 *
 * <p>Display fields only; emails and provider identifiers belong on {@link Identity}.
 */
public record User(
        UserId id,
        String displayName,
        String avatarUrl,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
    public User {
        if (id == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("User status is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps are required");
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public User withStatus(UserStatus next, Instant at) {
        return new User(id, displayName, avatarUrl, next, createdAt, at);
    }
}
