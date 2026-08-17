package id.belajarbersama.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * Link between an application {@link User} and an external identity provider subject.
 *
 * <p>Private. Must not be exposed on public learning or Q&amp;A projections.
 */
public record Identity(
        UUID id,
        UserId userId,
        IdentityProviderId provider,
        String issuer,
        String subject,
        Instant createdAt) {
    public Identity {
        if (id == null) {
            throw new IllegalArgumentException("Identity id is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Identity userId is required");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Identity provider is required");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Identity issuer is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Identity subject is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
