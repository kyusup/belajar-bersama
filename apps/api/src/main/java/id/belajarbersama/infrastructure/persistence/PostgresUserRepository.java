package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.identity.UserStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresUserRepository implements UserRepository {
    private static final String INSERT =
            """
            INSERT INTO app_user (id, display_name, avatar_url, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE =
            """
            UPDATE app_user SET display_name = ?, avatar_url = ?, status = ?, updated_at = ?
            WHERE id = ?
            """;
    private static final String SELECT =
            """
            SELECT id, display_name, avatar_url, status, created_at, updated_at
            FROM app_user WHERE id = ?
            """;

    private final DataSource dataSource;

    public PostgresUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(User user) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT)) {
            JdbcSupport.setUuid(statement, 1, user.id().value());
            statement.setString(2, user.displayName());
            statement.setString(3, user.avatarUrl());
            statement.setString(4, user.status().name());
            JdbcSupport.setInstant(statement, 5, user.createdAt());
            JdbcSupport.setInstant(statement, 6, user.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save user");
        }
    }

    @Override
    public void update(User user) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, user.displayName());
            statement.setString(2, user.avatarUrl());
            statement.setString(3, user.status().name());
            JdbcSupport.setInstant(statement, 4, user.updatedAt());
            JdbcSupport.setUuid(statement, 5, user.id().value());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update user");
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT)) {
            JdbcSupport.setUuid(statement, 1, id.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load user");
        }
    }

    @Override
    public List<User> search(String query, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 50);
        String like = like(query);
        String sql =
                SELECT.replace(" WHERE id = ?", "")
                        + (like == null ? "" : " WHERE display_name ILIKE ? ESCAPE '\\'")
                        + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (like != null) {
                statement.setString(index++, like);
            }
            statement.setInt(index++, pageSize);
            statement.setInt(index, pageNumber * pageSize);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to search users");
        }
    }

    @Override
    public long countSearch(String query) {
        String like = like(query);
        String sql =
                "SELECT COUNT(*) FROM app_user"
                        + (like == null ? "" : " WHERE display_name ILIKE ? ESCAPE '\\'");
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (like != null) {
                statement.setString(1, like);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to count users");
        }
    }

    private static String like(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.length() < 2) {
            return null;
        }
        return "%" + trimmed.replace("%", "\\%").replace("_", "\\_") + "%";
    }

    static User map(ResultSet resultSet) throws java.sql.SQLException {
        return new User(
                UserId.of(JdbcSupport.uuid(resultSet, "id")),
                resultSet.getString("display_name"),
                resultSet.getString("avatar_url"),
                UserStatus.valueOf(resultSet.getString("status")),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
