package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.learning.QuestionDifficulty;
import id.belajarbersama.domain.learning.QuestionType;
import id.belajarbersama.domain.learning.QuizOption;
import id.belajarbersama.domain.learning.QuizQuestion;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.domain.learning.QuizSpecRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresQuizSpecRepository implements QuizSpecRepository {
    private final DataSource dataSource;

    public PostgresQuizSpecRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void replace(QuizSpec spec) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement deleteQuestions =
                    connection.prepareStatement(
                            "DELETE FROM quiz_question WHERE revision_id = ?")) {
                JdbcSupport.setUuid(deleteQuestions, 1, spec.revisionId());
                deleteQuestions.executeUpdate();
            }
            try (PreparedStatement deleteSpec =
                    connection.prepareStatement("DELETE FROM quiz_spec WHERE revision_id = ?")) {
                JdbcSupport.setUuid(deleteSpec, 1, spec.revisionId());
                deleteSpec.executeUpdate();
            }
            try (PreparedStatement specStatement =
                    connection.prepareStatement(
                            "INSERT INTO quiz_spec (revision_id, passing_score, max_attempts, required) VALUES (?, ?, ?, ?)")) {
                JdbcSupport.setUuid(specStatement, 1, spec.revisionId());
                if (spec.passingScore() == null) {
                    specStatement.setObject(2, null);
                } else {
                    specStatement.setInt(2, spec.passingScore());
                }
                if (spec.maxAttempts() == null) {
                    specStatement.setObject(3, null);
                } else {
                    specStatement.setInt(3, spec.maxAttempts());
                }
                specStatement.setBoolean(4, spec.required());
                specStatement.executeUpdate();
            }
            String questionSql =
                    """
                    INSERT INTO quiz_question (
                        id, revision_id, sort_order, type, prompt, explanation, difficulty, competency_id, reference_note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            String optionSql =
                    """
                    INSERT INTO quiz_option (id, question_id, sort_order, label, body, correct)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement questionStatement = connection.prepareStatement(questionSql);
                    PreparedStatement optionStatement = connection.prepareStatement(optionSql)) {
                for (QuizQuestion question : spec.questions()) {
                    JdbcSupport.setUuid(questionStatement, 1, question.id());
                    JdbcSupport.setUuid(questionStatement, 2, spec.revisionId());
                    questionStatement.setInt(3, question.sortOrder());
                    questionStatement.setString(4, question.type().name());
                    questionStatement.setString(5, question.prompt());
                    questionStatement.setString(6, question.explanation());
                    questionStatement.setString(7, question.difficulty().name());
                    JdbcSupport.setUuid(questionStatement, 8, question.competencyId());
                    questionStatement.setString(9, question.reference());
                    questionStatement.executeUpdate();
                    for (QuizOption option : question.options()) {
                        JdbcSupport.setUuid(optionStatement, 1, option.id());
                        JdbcSupport.setUuid(optionStatement, 2, question.id());
                        optionStatement.setInt(3, option.sortOrder());
                        optionStatement.setString(4, option.label());
                        optionStatement.setString(5, option.text());
                        optionStatement.setBoolean(6, option.correct());
                        optionStatement.executeUpdate();
                    }
                }
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save quiz specification");
        }
    }

    @Override
    public Optional<QuizSpec> findByRevision(UUID revisionId) {
        String specSql =
                "SELECT revision_id, passing_score, max_attempts, required FROM quiz_spec WHERE revision_id = ?";
        String questionSql =
                """
                SELECT q.id, q.revision_id, q.sort_order, q.type, q.prompt, q.explanation, q.difficulty,
                       q.competency_id, q.reference_note, o.id AS option_id, o.sort_order AS option_sort,
                       o.label, o.body, o.correct
                FROM quiz_question q
                LEFT JOIN quiz_option o ON o.question_id = q.id
                WHERE q.revision_id = ?
                ORDER BY q.sort_order, o.sort_order
                """;
        try (Connection connection = dataSource.getConnection()) {
            Integer passing = null;
            Integer maxAttempts = null;
            boolean required = true;
            boolean found = false;
            try (PreparedStatement statement = connection.prepareStatement(specSql)) {
                JdbcSupport.setUuid(statement, 1, revisionId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        found = true;
                        passing = (Integer) resultSet.getObject("passing_score");
                        maxAttempts = (Integer) resultSet.getObject("max_attempts");
                        required = resultSet.getBoolean("required");
                    }
                }
            }
            if (!found) {
                return Optional.empty();
            }
            Map<UUID, QuizQuestion> questions = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(questionSql)) {
                JdbcSupport.setUuid(statement, 1, revisionId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID questionId = JdbcSupport.uuid(resultSet, "id");
                        QuizQuestion current =
                                questions.computeIfAbsent(
                                        questionId,
                                        id -> {
                                            try {
                                                return new QuizQuestion(
                                                        id,
                                                        JdbcSupport.uuid(resultSet, "revision_id"),
                                                        resultSet.getInt("sort_order"),
                                                        QuestionType.valueOf(
                                                                resultSet.getString("type")),
                                                        resultSet.getString("prompt"),
                                                        resultSet.getString("explanation"),
                                                        QuestionDifficulty.valueOf(
                                                                resultSet.getString("difficulty")),
                                                        JdbcSupport.uuid(
                                                                resultSet, "competency_id"),
                                                        resultSet.getString("reference_note"),
                                                        new ArrayList<>());
                                            } catch (Exception exception) {
                                                throw JdbcSupport.wrap(
                                                        exception, "Failed to map question");
                                            }
                                        });
                        UUID optionId = JdbcSupport.uuid(resultSet, "option_id");
                        if (optionId != null) {
                            current.options()
                                    .add(
                                            new QuizOption(
                                                    optionId,
                                                    questionId,
                                                    resultSet.getInt("option_sort"),
                                                    resultSet.getString("label"),
                                                    resultSet.getString("body"),
                                                    resultSet.getBoolean("correct")));
                        }
                    }
                }
            }
            List<QuizQuestion> list =
                    questions.values().stream()
                            .map(
                                    item ->
                                            new QuizQuestion(
                                                    item.id(),
                                                    item.revisionId(),
                                                    item.sortOrder(),
                                                    item.type(),
                                                    item.prompt(),
                                                    item.explanation(),
                                                    item.difficulty(),
                                                    item.competencyId(),
                                                    item.reference(),
                                                    List.copyOf(item.options())))
                            .toList();
            return Optional.of(new QuizSpec(revisionId, passing, maxAttempts, required, list));
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load quiz specification");
        }
    }
}
