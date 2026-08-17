package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.IdentityProviderId;
import id.belajarbersama.domain.identity.OauthState;
import id.belajarbersama.domain.identity.OauthStateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresOauthStateRepository implements OauthStateRepository {
    private final DataSource dataSource;

    public PostgresOauthStateRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(OauthState state) {
        String sql =
                """
                INSERT INTO oauth_state (state, provider, code_verifier, nonce, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.state());
            statement.setString(2, state.provider().name());
            statement.setString(3, state.codeVerifier());
            statement.setString(4, state.nonce());
            JdbcSupport.setInstant(statement, 5, state.expiresAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save oauth state");
        }
    }

    @Override
    public Optional<OauthState> consume(String state) {
        String select =
                """
                SELECT state, provider, code_verifier, nonce, expires_at
                FROM oauth_state WHERE state = ?
                """;
        try (Connection connection = dataSource.getConnection()) {
            OauthState found;
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setString(1, state);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    found =
                            new OauthState(
                                    resultSet.getString("state"),
                                    IdentityProviderId.valueOf(resultSet.getString("provider")),
                                    resultSet.getString("code_verifier"),
                                    resultSet.getString("nonce"),
                                    JdbcSupport.instant(resultSet, "expires_at"));
                }
            }
            try (PreparedStatement delete =
                    connection.prepareStatement("DELETE FROM oauth_state WHERE state = ?")) {
                delete.setString(1, state);
                delete.executeUpdate();
            }
            return Optional.of(found);
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to consume oauth state");
        }
    }
}
