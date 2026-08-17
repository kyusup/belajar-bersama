package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.ContentSubmissionRepository;
import id.belajarbersama.domain.content.SubmissionStatus;
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
public class PostgresContentSubmissionRepository implements ContentSubmissionRepository {
    private static final String SELECT =
            """
            SELECT id, content_id, revision_id, maker_id, status, assigned_checker_id, assigned_by,
                   assigned_at, version, created_at, updated_at
            FROM content_submission
            """;

    private final DataSource dataSource;

    public PostgresContentSubmissionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(ContentSubmission submission) {
        String sql =
                """
                INSERT INTO content_submission (
                    id, content_id, revision_id, maker_id, status, assigned_checker_id, assigned_by,
                    assigned_at, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, submission);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save submission");
        }
    }

    @Override
    public boolean updateIfVersion(ContentSubmission submission, int expectedVersion) {
        String sql =
                """
                UPDATE content_submission SET
                    revision_id = ?, status = ?, assigned_checker_id = ?, assigned_by = ?,
                    assigned_at = ?, version = ?, updated_at = ?
                WHERE id = ? AND version = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, submission.revisionId());
            statement.setString(2, submission.status().name());
            JdbcSupport.setUuid(
                    statement,
                    3,
                    submission.assignedCheckerId() == null
                            ? null
                            : submission.assignedCheckerId().value());
            JdbcSupport.setUuid(
                    statement,
                    4,
                    submission.assignedBy() == null ? null : submission.assignedBy().value());
            JdbcSupport.setInstant(statement, 5, submission.assignedAt());
            statement.setInt(6, expectedVersion + 1);
            JdbcSupport.setInstant(statement, 7, submission.updatedAt());
            JdbcSupport.setUuid(statement, 8, submission.id());
            statement.setInt(9, expectedVersion);
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update submission");
        }
    }

    @Override
    public Optional<ContentSubmission> findById(UUID id) {
        return one(SELECT + " WHERE id = ?", id);
    }

    @Override
    public Optional<ContentSubmission> findOpenByContent(UUID contentId) {
        String sql =
                SELECT
                        + " WHERE content_id = ? AND status IN ('SUBMITTED', 'IN_REVIEW') ORDER BY created_at DESC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load open submission");
        }
    }

    @Override
    public List<ContentSubmission> listQueue() {
        return list(SELECT + " WHERE status = 'SUBMITTED' ORDER BY created_at");
    }

    @Override
    public List<ContentSubmission> listAssignedTo(UserId checkerId) {
        String sql =
                SELECT
                        + " WHERE assigned_checker_id = ? AND status IN ('SUBMITTED', 'IN_REVIEW') ORDER BY updated_at DESC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, checkerId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentSubmission> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list assigned submissions");
        }
    }

    @Override
    public List<ContentSubmission> listByContent(UUID contentId) {
        String sql = SELECT + " WHERE content_id = ? ORDER BY created_at";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentSubmission> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list submissions");
        }
    }

    private Optional<ContentSubmission> one(String sql, UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load submission");
        }
    }

    private List<ContentSubmission> list(String sql) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<ContentSubmission> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list submissions");
        }
    }

    private void bind(PreparedStatement statement, ContentSubmission submission) throws Exception {
        JdbcSupport.setUuid(statement, 1, submission.id());
        JdbcSupport.setUuid(statement, 2, submission.contentId());
        JdbcSupport.setUuid(statement, 3, submission.revisionId());
        JdbcSupport.setUuid(statement, 4, submission.makerId().value());
        statement.setString(5, submission.status().name());
        JdbcSupport.setUuid(
                statement,
                6,
                submission.assignedCheckerId() == null
                        ? null
                        : submission.assignedCheckerId().value());
        JdbcSupport.setUuid(
                statement,
                7,
                submission.assignedBy() == null ? null : submission.assignedBy().value());
        JdbcSupport.setInstant(statement, 8, submission.assignedAt());
        statement.setInt(9, submission.version());
        JdbcSupport.setInstant(statement, 10, submission.createdAt());
        JdbcSupport.setInstant(statement, 11, submission.updatedAt());
    }

    private static ContentSubmission map(ResultSet resultSet) throws java.sql.SQLException {
        UUID assigned = JdbcSupport.uuid(resultSet, "assigned_checker_id");
        UUID assignedBy = JdbcSupport.uuid(resultSet, "assigned_by");
        return new ContentSubmission(
                JdbcSupport.uuid(resultSet, "id"),
                JdbcSupport.uuid(resultSet, "content_id"),
                JdbcSupport.uuid(resultSet, "revision_id"),
                UserId.of(JdbcSupport.uuid(resultSet, "maker_id")),
                SubmissionStatus.valueOf(resultSet.getString("status")),
                assigned == null ? null : UserId.of(assigned),
                assignedBy == null ? null : UserId.of(assignedBy),
                JdbcSupport.instant(resultSet, "assigned_at"),
                resultSet.getInt("version"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
