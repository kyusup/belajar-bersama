package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.LearningActivityKind;
import id.belajarbersama.domain.learning.LearningActivityRepository;
import id.belajarbersama.domain.learning.LearningResume;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresLearningActivityRepository implements LearningActivityRepository {
    private final DataSource dataSource;

    public PostgresLearningActivityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(
            UUID id, UserId userId, UUID contentId, LearningActivityKind kind, Instant at) {
        String sql =
                "INSERT INTO learning_activity (id, user_id, content_id, kind, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, id);
            JdbcSupport.setUuid(statement, 2, userId.value());
            JdbcSupport.setUuid(statement, 3, contentId);
            statement.setString(4, kind.name());
            JdbcSupport.setInstant(statement, 5, at);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save learning activity");
        }
    }

    @Override
    public Optional<LearningResume> resume(UserId userId) {
        String sql =
                "SELECT user_id, content_id, course_id, updated_at FROM learning_resume WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new LearningResume(
                                UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                                JdbcSupport.uuid(resultSet, "content_id"),
                                JdbcSupport.uuid(resultSet, "course_id"),
                                JdbcSupport.instant(resultSet, "updated_at")));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load learning resume");
        }
    }

    @Override
    public void upsertResume(LearningResume resume) {
        String sql =
                """
                INSERT INTO learning_resume (user_id, content_id, course_id, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET content_id = EXCLUDED.content_id,
                    course_id = EXCLUDED.course_id, updated_at = EXCLUDED.updated_at
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, resume.userId().value());
            JdbcSupport.setUuid(statement, 2, resume.contentId());
            JdbcSupport.setUuid(statement, 3, resume.courseId());
            JdbcSupport.setInstant(statement, 4, resume.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save learning resume");
        }
    }
}
