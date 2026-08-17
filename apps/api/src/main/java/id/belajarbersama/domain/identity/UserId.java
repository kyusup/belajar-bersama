package id.belajarbersama.domain.identity;

import java.util.Objects;
import java.util.UUID;

/** Application user identifier. Distinct from an identity-provider subject. */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId value is required");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
