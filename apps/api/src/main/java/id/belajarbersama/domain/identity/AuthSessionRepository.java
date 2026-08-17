package id.belajarbersama.domain.identity;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository {
    void save(AuthSession session);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    void revoke(java.util.UUID sessionId, Instant at);

    void revokeAllForUser(UserId userId, Instant at);
}
