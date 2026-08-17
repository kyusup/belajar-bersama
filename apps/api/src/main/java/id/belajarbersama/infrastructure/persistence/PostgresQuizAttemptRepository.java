package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.AttemptAnswers;
import id.belajarbersama.domain.learning.AttemptStatus;
import id.belajarbersama.domain.learning.QuizAttempt;
import id.belajarbersama.domain.learning.QuizAttemptRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresQuizAttemptRepository implements QuizAttemptRepository {
    private static final String SELECT =
            """
            SELECT id, user_id, quiz_id, quiz_revision_id, status, score_percent, passed,
                   correct_count, question_count, version, started_at, submitted_at
            FROM quiz_attempt
            """;

    private final DataSource dataSource;

    public PostgresQuizAttemptRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(QuizAttempt attempt) {
        String sql =
                """
                INSERT INTO quiz_attempt (
                    id, user_id, quiz_id, quiz_revision_id, status, score_percent, passed,
                    correct_count, question_count, version, started_at, submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, attempt);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save quiz attempt");
        }
    }

    @Override
    public boolean updateIfVersion(QuizAttempt attempt, int expectedVersion) {
        String sql =
                """
                UPDATE quiz_attempt SET status = ?, score_percent = ?, passed = ?, correct_count = ?,
                    question_count = ?, version = ?, submitted_at = ?
                WHERE id = ? AND version = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attempt.status().name());
            setNullableInt(statement, 2, attempt.scorePercent());
            if (attempt.passed() == null) {
                statement.setObject(3, null);
            } else {
                statement.setBoolean(3, attempt.passed());
            }
            setNullableInt(statement, 4, attempt.correctCount());
            setNullableInt(statement, 5, attempt.questionCount());
            statement.setInt(6, expectedVersion + 1);
            JdbcSupport.setInstant(statement, 7, attempt.submittedAt());
            JdbcSupport.setUuid(statement, 8, attempt.id());
            statement.setInt(9, expectedVersion);
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update quiz attempt");
        }
    }

    @Override
    public Optional<QuizAttempt> findById(UUID id) {
        return one(SELECT + " WHERE id = ?", statement -> JdbcSupport.setUuid(statement, 1, id));
    }

    @Override
    public Optional<QuizAttempt> findOpen(UserId userId, UUID quizId) {
        return one(
                SELECT + " WHERE user_id = ? AND quiz_id = ? AND status = 'IN_PROGRESS'",
                statement -> {
                    JdbcSupport.setUuid(statement, 1, userId.value());
                    JdbcSupport.setUuid(statement, 2, quizId);
                });
    }

    @Override
    public List<QuizAttempt> listByUserAndQuiz(UserId userId, UUID quizId) {
        return list(
                SELECT + " WHERE user_id = ? AND quiz_id = ? ORDER BY started_at DESC",
                statement -> {
                    JdbcSupport.setUuid(statement, 1, userId.value());
                    JdbcSupport.setUuid(statement, 2, quizId);
                });
    }

    @Override
    public List<QuizAttempt> listRecentSubmitted(UserId userId, int limit) {
        return list(
                SELECT
                        + " WHERE user_id = ? AND status = 'SUBMITTED' ORDER BY submitted_at DESC LIMIT ?",
                statement -> {
                    JdbcSupport.setUuid(statement, 1, userId.value());
                    statement.setInt(2, limit);
                });
    }

    @Override
    public int countActive(UserId userId, UUID quizId) {
        String sql =
                "SELECT COUNT(*) FROM quiz_attempt WHERE user_id = ? AND quiz_id = ? AND status IN ('IN_PROGRESS', 'SUBMITTED')";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to count quiz attempts");
        }
    }

    @Override
    public boolean hasPassingSubmission(UserId userId, UUID quizId) {
        String sql =
                "SELECT 1 FROM quiz_attempt WHERE user_id = ? AND quiz_id = ? AND status = 'SUBMITTED' AND passed IS TRUE";
        return exists(sql, userId, quizId);
    }

    @Override
    public boolean hasSubmittedAttempt(UserId userId, UUID quizId) {
        String sql =
                "SELECT 1 FROM quiz_attempt WHERE user_id = ? AND quiz_id = ? AND status = 'SUBMITTED'";
        return exists(sql, userId, quizId);
    }

    @Override
    public AttemptAnswers answers(UUID attemptId) {
        String sql = "SELECT question_id, option_id FROM quiz_answer_option WHERE attempt_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, attemptId);
            Map<UUID, Set<UUID>> map = new HashMap<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    map.computeIfAbsent(
                                    JdbcSupport.uuid(resultSet, "question_id"),
                                    key -> new HashSet<>())
                            .add(JdbcSupport.uuid(resultSet, "option_id"));
                }
            }
            return new AttemptAnswers(map);
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load quiz answers");
        }
    }

    @Override
    public void replaceAnswers(UUID attemptId, AttemptAnswers answers) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement delete =
                    connection.prepareStatement(
                            "DELETE FROM quiz_answer_option WHERE attempt_id = ?")) {
                JdbcSupport.setUuid(delete, 1, attemptId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert =
                    connection.prepareStatement(
                            "INSERT INTO quiz_answer_option (attempt_id, question_id, option_id) VALUES (?, ?, ?)")) {
                for (var entry : answers.selectedByQuestion().entrySet()) {
                    for (UUID optionId : entry.getValue()) {
                        JdbcSupport.setUuid(insert, 1, attemptId);
                        JdbcSupport.setUuid(insert, 2, entry.getKey());
                        JdbcSupport.setUuid(insert, 3, optionId);
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save quiz answers");
        }
    }

    private boolean exists(String sql, UserId userId, UUID quizId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to query quiz attempts");
        }
    }

    private void bind(PreparedStatement statement, QuizAttempt attempt) throws Exception {
        JdbcSupport.setUuid(statement, 1, attempt.id());
        JdbcSupport.setUuid(statement, 2, attempt.userId().value());
        JdbcSupport.setUuid(statement, 3, attempt.quizId());
        JdbcSupport.setUuid(statement, 4, attempt.quizRevisionId());
        statement.setString(5, attempt.status().name());
        setNullableInt(statement, 6, attempt.scorePercent());
        if (attempt.passed() == null) {
            statement.setObject(7, null);
        } else {
            statement.setBoolean(7, attempt.passed());
        }
        setNullableInt(statement, 8, attempt.correctCount());
        setNullableInt(statement, 9, attempt.questionCount());
        statement.setInt(10, attempt.version());
        JdbcSupport.setInstant(statement, 11, attempt.startedAt());
        JdbcSupport.setInstant(statement, 12, attempt.submittedAt());
    }

    private Optional<QuizAttempt> one(String sql, Binder binder) {
        List<QuizAttempt> items = list(sql, binder);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    private List<QuizAttempt> list(String sql, Binder binder) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QuizAttempt> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load quiz attempts");
        }
    }

    private static QuizAttempt map(ResultSet resultSet) throws java.sql.SQLException {
        Boolean passed = (Boolean) resultSet.getObject("passed");
        return new QuizAttempt(
                JdbcSupport.uuid(resultSet, "id"),
                UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                JdbcSupport.uuid(resultSet, "quiz_id"),
                JdbcSupport.uuid(resultSet, "quiz_revision_id"),
                AttemptStatus.valueOf(resultSet.getString("status")),
                (Integer) resultSet.getObject("score_percent"),
                passed,
                (Integer) resultSet.getObject("correct_count"),
                (Integer) resultSet.getObject("question_count"),
                resultSet.getInt("version"),
                JdbcSupport.instant(resultSet, "started_at"),
                JdbcSupport.instant(resultSet, "submitted_at"));
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setInt(index, value);
        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
