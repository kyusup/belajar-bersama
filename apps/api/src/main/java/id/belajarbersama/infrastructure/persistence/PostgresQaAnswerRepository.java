package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.qa.QaAnswer;
import id.belajarbersama.domain.qa.QaAnswerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresQaAnswerRepository implements QaAnswerRepository {
    private static final String SELECT =
            """
            SELECT id, question_id, author_id, body, hidden, created_at, updated_at
            FROM qa_answer
            """;

    private final DataSource dataSource;

    public PostgresQaAnswerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(QaAnswer answer) {
        String sql =
                """
                INSERT INTO qa_answer (id, question_id, author_id, body, hidden, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, answer.id());
            JdbcSupport.setUuid(statement, 2, answer.questionId());
            JdbcSupport.setUuid(statement, 3, answer.authorId().value());
            statement.setString(4, answer.body());
            statement.setBoolean(5, answer.hidden());
            JdbcSupport.setInstant(statement, 6, answer.createdAt());
            JdbcSupport.setInstant(statement, 7, answer.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save answer");
        }
    }

    @Override
    public void update(QaAnswer answer) {
        String sql = "UPDATE qa_answer SET body = ?, hidden = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, answer.body());
            statement.setBoolean(2, answer.hidden());
            JdbcSupport.setInstant(statement, 3, answer.updatedAt());
            JdbcSupport.setUuid(statement, 4, answer.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update answer");
        }
    }

    @Override
    public Optional<QaAnswer> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load answer");
        }
    }

    @Override
    public List<QaAnswer> listByQuestion(UUID questionId, boolean includeHidden) {
        String sql =
                SELECT
                        + " WHERE question_id = ?"
                        + (includeHidden ? "" : " AND hidden = FALSE")
                        + " ORDER BY created_at ASC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaAnswer> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list answers");
        }
    }

    @Override
    public int usefulCount(UUID answerId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT COUNT(*) FROM qa_answer_useful WHERE answer_id = ?")) {
            JdbcSupport.setUuid(statement, 1, answerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to count useful marks");
        }
    }

    @Override
    public boolean markedUseful(UserId userId, UUID answerId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT 1 FROM qa_answer_useful WHERE user_id = ? AND answer_id = ?")) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, answerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load useful mark");
        }
    }

    @Override
    public boolean addUseful(UserId userId, UUID answerId) {
        String sql =
                """
                INSERT INTO qa_answer_useful (user_id, answer_id, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, answerId);
            JdbcSupport.setInstant(statement, 3, Instant.now());
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to mark useful");
        }
    }

    @Override
    public void removeUseful(UserId userId, UUID answerId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM qa_answer_useful WHERE user_id = ? AND answer_id = ?")) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, answerId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to remove useful mark");
        }
    }

    private static QaAnswer map(ResultSet resultSet) throws java.sql.SQLException {
        return new QaAnswer(
                JdbcSupport.uuid(resultSet, "id"),
                JdbcSupport.uuid(resultSet, "question_id"),
                UserId.of(JdbcSupport.uuid(resultSet, "author_id")),
                resultSet.getString("body"),
                resultSet.getBoolean("hidden"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
