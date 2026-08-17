package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ContentReview;
import id.belajarbersama.domain.content.ContentReviewRepository;
import id.belajarbersama.domain.content.ReviewDecision;
import id.belajarbersama.domain.identity.UserId;
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
public class PostgresContentReviewRepository implements ContentReviewRepository {
    private static final String SELECT =
            """
            SELECT id, submission_id, revision_id, reviewer_id, decision, comment, created_at, decided_at
            FROM content_review
            """;

    private final DataSource dataSource;

    public PostgresContentReviewRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(ContentReview review) {
        String sql =
                """
                INSERT INTO content_review (
                    id, submission_id, revision_id, reviewer_id, decision, comment, created_at, decided_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, review.id());
            JdbcSupport.setUuid(statement, 2, review.submissionId());
            JdbcSupport.setUuid(statement, 3, review.revisionId());
            JdbcSupport.setUuid(statement, 4, review.reviewerId().value());
            statement.setString(5, review.decision() == null ? null : review.decision().name());
            statement.setString(6, review.comment());
            JdbcSupport.setInstant(statement, 7, review.createdAt());
            JdbcSupport.setInstant(statement, 8, review.decidedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save review");
        }
    }

    @Override
    public void update(ContentReview review) {
        String sql =
                """
                UPDATE content_review SET decision = ?, comment = ?, decided_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, review.decision() == null ? null : review.decision().name());
            statement.setString(2, review.comment());
            JdbcSupport.setInstant(statement, 3, review.decidedAt());
            JdbcSupport.setUuid(statement, 4, review.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update review");
        }
    }

    @Override
    public Optional<ContentReview> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load review");
        }
    }

    @Override
    public List<ContentReview> listBySubmission(UUID submissionId) {
        return list(SELECT + " WHERE submission_id = ? ORDER BY created_at", submissionId);
    }

    @Override
    public List<ContentReview> listByContent(UUID contentId) {
        String sql =
                SELECT
                        + " WHERE submission_id IN (SELECT id FROM content_submission WHERE content_id = ?) ORDER BY created_at";
        return list(sql, contentId);
    }

    private List<ContentReview> list(String sql, UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentReview> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list reviews");
        }
    }

    private static ContentReview map(ResultSet resultSet) throws java.sql.SQLException {
        String decision = resultSet.getString("decision");
        return new ContentReview(
                JdbcSupport.uuid(resultSet, "id"),
                JdbcSupport.uuid(resultSet, "submission_id"),
                JdbcSupport.uuid(resultSet, "revision_id"),
                UserId.of(JdbcSupport.uuid(resultSet, "reviewer_id")),
                decision == null ? null : ReviewDecision.valueOf(decision),
                resultSet.getString("comment"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "decided_at"));
    }
}
