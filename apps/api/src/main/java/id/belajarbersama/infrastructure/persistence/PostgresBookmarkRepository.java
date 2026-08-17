package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.Bookmark;
import id.belajarbersama.domain.learning.BookmarkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresBookmarkRepository implements BookmarkRepository {
    private final DataSource dataSource;

    public PostgresBookmarkRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Bookmark bookmark) {
        String sql =
                """
                INSERT INTO bookmark (user_id, content_id, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, content_id) DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, bookmark.userId().value());
            JdbcSupport.setUuid(statement, 2, bookmark.contentId());
            JdbcSupport.setInstant(statement, 3, bookmark.createdAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save bookmark");
        }
    }

    @Override
    public void delete(UserId userId, UUID contentId) {
        String sql = "DELETE FROM bookmark WHERE user_id = ? AND content_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, contentId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to delete bookmark");
        }
    }

    @Override
    public boolean exists(UserId userId, UUID contentId) {
        String sql = "SELECT 1 FROM bookmark WHERE user_id = ? AND content_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load bookmark");
        }
    }

    @Override
    public List<Bookmark> listByUser(UserId userId) {
        String sql =
                "SELECT user_id, content_id, created_at FROM bookmark WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Bookmark> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(
                            new Bookmark(
                                    UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                                    JdbcSupport.uuid(resultSet, "content_id"),
                                    JdbcSupport.instant(resultSet, "created_at")));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list bookmarks");
        }
    }
}
