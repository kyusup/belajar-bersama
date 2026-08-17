package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.qa.QaQuestion;
import id.belajarbersama.domain.qa.QaQuestionRepository;
import id.belajarbersama.domain.qa.QaStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresQaQuestionRepository implements QaQuestionRepository {
    private static final String SELECT =
            """
            SELECT id, author_id, title, body, subject_id, content_id, status, accepted_answer_id,
                   created_at, updated_at
            FROM qa_question
            """;

    private final DataSource dataSource;

    public PostgresQaQuestionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(QaQuestion question) {
        String sql =
                """
                INSERT INTO qa_question (
                    id, author_id, title, body, subject_id, content_id, status, accepted_answer_id,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, question);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save question");
        }
        index(question);
    }

    @Override
    public void update(QaQuestion question) {
        String sql =
                """
                UPDATE qa_question
                SET title = ?, body = ?, subject_id = ?, content_id = ?, status = ?,
                    accepted_answer_id = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, question.title());
            statement.setString(2, question.body());
            JdbcSupport.setUuid(statement, 3, question.subjectId());
            JdbcSupport.setUuid(statement, 4, question.contentId());
            statement.setString(5, question.status().name());
            JdbcSupport.setUuid(statement, 6, question.acceptedAnswerId());
            JdbcSupport.setInstant(statement, 7, question.updatedAt());
            JdbcSupport.setUuid(statement, 8, question.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update question");
        }
        if (question.publiclyVisible()) {
            index(question);
        } else {
            deleteIndex(question.id());
        }
    }

    @Override
    public Optional<QaQuestion> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load question");
        }
    }

    @Override
    public List<QaQuestion> listPublic(UUID contentId, UUID subjectId, int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE status IN ('OPEN', 'CLOSED')");
        if (contentId != null) {
            sql.append(" AND content_id = ?");
        }
        if (subjectId != null) {
            sql.append(" AND subject_id = ?");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (contentId != null) {
                JdbcSupport.setUuid(statement, index++, contentId);
            }
            if (subjectId != null) {
                JdbcSupport.setUuid(statement, index++, subjectId);
            }
            statement.setInt(index++, size);
            statement.setInt(index, page * size);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaQuestion> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list questions");
        }
    }

    @Override
    public long countPublic(UUID contentId, UUID subjectId) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT COUNT(*) FROM qa_question WHERE status IN ('OPEN', 'CLOSED')");
        if (contentId != null) {
            sql.append(" AND content_id = ?");
        }
        if (subjectId != null) {
            sql.append(" AND subject_id = ?");
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (contentId != null) {
                JdbcSupport.setUuid(statement, index++, contentId);
            }
            if (subjectId != null) {
                JdbcSupport.setUuid(statement, index, subjectId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to count questions");
        }
    }

    @Override
    public void index(QaQuestion question) {
        String sql =
                """
                INSERT INTO qa_search (question_id, title, body_text)
                VALUES (?, ?, ?)
                ON CONFLICT (question_id) DO UPDATE SET title = EXCLUDED.title, body_text = EXCLUDED.body_text
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, question.id());
            statement.setString(2, question.title());
            statement.setString(3, question.body());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to index question");
        }
    }

    @Override
    public void deleteIndex(UUID questionId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM qa_search WHERE question_id = ?")) {
            JdbcSupport.setUuid(statement, 1, questionId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to delete question index");
        }
    }

    private static void bind(PreparedStatement statement, QaQuestion question) throws Exception {
        JdbcSupport.setUuid(statement, 1, question.id());
        JdbcSupport.setUuid(statement, 2, question.authorId().value());
        statement.setString(3, question.title());
        statement.setString(4, question.body());
        JdbcSupport.setUuid(statement, 5, question.subjectId());
        JdbcSupport.setUuid(statement, 6, question.contentId());
        statement.setString(7, question.status().name());
        JdbcSupport.setUuid(statement, 8, question.acceptedAnswerId());
        JdbcSupport.setInstant(statement, 9, question.createdAt());
        JdbcSupport.setInstant(statement, 10, question.updatedAt());
    }

    private static QaQuestion map(ResultSet resultSet) throws java.sql.SQLException {
        return new QaQuestion(
                JdbcSupport.uuid(resultSet, "id"),
                UserId.of(JdbcSupport.uuid(resultSet, "author_id")),
                resultSet.getString("title"),
                resultSet.getString("body"),
                JdbcSupport.uuid(resultSet, "subject_id"),
                JdbcSupport.uuid(resultSet, "content_id"),
                QaStatus.valueOf(resultSet.getString("status")),
                JdbcSupport.uuid(resultSet, "accepted_answer_id"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
