package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.LessonCompletion;
import id.belajarbersama.domain.learning.LessonCompletionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresLessonCompletionRepository implements LessonCompletionRepository {
    private final DataSource dataSource;

    public PostgresLessonCompletionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean complete(LessonCompletion completion) {
        String sql =
                """
                INSERT INTO lesson_completion (user_id, content_id, revision_id, completed_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, content_id) DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, completion.userId().value());
            JdbcSupport.setUuid(statement, 2, completion.contentId());
            JdbcSupport.setUuid(statement, 3, completion.revisionId());
            JdbcSupport.setInstant(statement, 4, completion.completedAt());
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to record lesson completion");
        }
    }

    @Override
    public boolean exists(UserId userId, UUID contentId) {
        String sql = "SELECT 1 FROM lesson_completion WHERE user_id = ? AND content_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load lesson completion");
        }
    }

    @Override
    public Set<UUID> completedContentIds(UserId userId, List<UUID> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Set.of();
        }
        StringBuilder sql =
                new StringBuilder(
                        "SELECT content_id FROM lesson_completion WHERE user_id = ? AND content_id IN (");
        sql.append("?,".repeat(contentIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            int index = 2;
            for (UUID id : contentIds) {
                JdbcSupport.setUuid(statement, index++, id);
            }
            Set<UUID> ids = new HashSet<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(JdbcSupport.uuid(resultSet, "content_id"));
                }
            }
            return ids;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list lesson completions");
        }
    }

    @Override
    public List<LessonCompletion> listByUser(UserId userId) {
        String sql =
                "SELECT user_id, content_id, revision_id, completed_at FROM lesson_completion WHERE user_id = ? ORDER BY completed_at DESC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<LessonCompletion> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(
                            new LessonCompletion(
                                    UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                                    JdbcSupport.uuid(resultSet, "content_id"),
                                    JdbcSupport.uuid(resultSet, "revision_id"),
                                    JdbcSupport.instant(resultSet, "completed_at")));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list lesson completions");
        }
    }
}
