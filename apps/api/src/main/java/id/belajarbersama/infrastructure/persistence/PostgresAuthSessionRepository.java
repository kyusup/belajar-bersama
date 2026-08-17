package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.AuthSession;
import id.belajarbersama.domain.identity.AuthSessionRepository;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresAuthSessionRepository implements AuthSessionRepository {
    private final DataSource dataSource;

    public PostgresAuthSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(AuthSession session) {
        String sql =
                """
                INSERT INTO auth_session (id, user_id, token_hash, expires_at, created_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, session.id());
            JdbcSupport.setUuid(statement, 2, session.userId().value());
            statement.setString(3, session.tokenHash());
            JdbcSupport.setInstant(statement, 4, session.expiresAt());
            JdbcSupport.setInstant(statement, 5, session.createdAt());
            JdbcSupport.setInstant(statement, 6, session.revokedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save session");
        }
    }

    @Override
    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        String sql =
                """
                SELECT id, user_id, token_hash, expires_at, created_at, revoked_at
                FROM auth_session WHERE token_hash = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new AuthSession(
                                JdbcSupport.uuid(resultSet, "id"),
                                UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                                resultSet.getString("token_hash"),
                                JdbcSupport.instant(resultSet, "expires_at"),
                                JdbcSupport.instant(resultSet, "created_at"),
                                JdbcSupport.instant(resultSet, "revoked_at")));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load session");
        }
    }

    @Override
    public void revoke(UUID sessionId, Instant at) {
        String sql = "UPDATE auth_session SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, at);
            JdbcSupport.setUuid(statement, 2, sessionId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to revoke session");
        }
    }

    @Override
    public void revokeAllForUser(UserId userId, Instant at) {
        String sql =
                "UPDATE auth_session SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setInstant(statement, 1, at);
            JdbcSupport.setUuid(statement, 2, userId.value());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to revoke user sessions");
        }
    }
}
