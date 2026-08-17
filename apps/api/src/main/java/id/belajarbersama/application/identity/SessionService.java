package id.belajarbersama.application.identity;

import id.belajarbersama.domain.identity.AuthSession;
import id.belajarbersama.domain.identity.AuthSessionRepository;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SessionService {
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public SessionService(
            AuthSessionRepository sessions,
            UserRepository users,
            @ConfigProperty(name = "bb.auth.session-ttl-hours", defaultValue = "168")
                    int ttlHours) {
        this.sessions = sessions;
        this.users = users;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public IssuedSession issue(UserId userId) {
        Instant now = Instant.now();
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        AuthSession session =
                new AuthSession(UUID.randomUUID(), userId, hash(token), now.plus(ttl), now, null);
        sessions.save(session);
        return new IssuedSession(token, session);
    }

    public Optional<AuthSession> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sessions.findByTokenHash(hash(token))
                .filter(session -> session.isUsable(Instant.now()))
                .filter(
                        session ->
                                users.findById(session.userId()).map(User::isActive).orElse(false));
    }

    public void revoke(String token) {
        resolve(token).ifPresent(session -> sessions.revoke(session.id(), Instant.now()));
    }

    public void revokeAll(UserId userId) {
        sessions.revokeAllForUser(userId, Instant.now());
    }

    public Duration ttl() {
        return ttl;
    }

    public static String hash(String token) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record IssuedSession(String token, AuthSession session) {}
}
