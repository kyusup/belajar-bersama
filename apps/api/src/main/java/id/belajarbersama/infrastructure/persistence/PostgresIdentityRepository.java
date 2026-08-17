package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.Identity;
import id.belajarbersama.domain.identity.IdentityProviderId;
import id.belajarbersama.domain.identity.IdentityRepository;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresIdentityRepository implements IdentityRepository {
    private static final String INSERT =
            """
            INSERT INTO identity_link (id, user_id, provider, issuer, subject, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String BY_SUBJECT =
            """
            SELECT id, user_id, provider, issuer, subject, created_at
            FROM identity_link WHERE provider = ? AND issuer = ? AND subject = ?
            """;
    private static final String BY_USER =
            """
            SELECT id, user_id, provider, issuer, subject, created_at
            FROM identity_link WHERE user_id = ? ORDER BY created_at
            """;

    private final DataSource dataSource;

    public PostgresIdentityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Identity identity) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT)) {
            JdbcSupport.setUuid(statement, 1, identity.id());
            JdbcSupport.setUuid(statement, 2, identity.userId().value());
            statement.setString(3, identity.provider().name());
            statement.setString(4, identity.issuer());
            statement.setString(5, identity.subject());
            JdbcSupport.setInstant(statement, 6, identity.createdAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save identity");
        }
    }

    @Override
    public Optional<Identity> findByProviderSubject(
            IdentityProviderId provider, String issuer, String subject) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(BY_SUBJECT)) {
            statement.setString(1, provider.name());
            statement.setString(2, issuer);
            statement.setString(3, subject);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load identity");
        }
    }

    @Override
    public List<Identity> listByUser(UserId userId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(BY_USER)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Identity> identities = new ArrayList<>();
                while (resultSet.next()) {
                    identities.add(map(resultSet));
                }
                return identities;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list identities");
        }
    }

    private static Identity map(ResultSet resultSet) throws java.sql.SQLException {
        return new Identity(
                JdbcSupport.uuid(resultSet, "id"),
                UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                IdentityProviderId.valueOf(resultSet.getString("provider")),
                resultSet.getString("issuer"),
                resultSet.getString("subject"),
                JdbcSupport.instant(resultSet, "created_at"));
    }
}
