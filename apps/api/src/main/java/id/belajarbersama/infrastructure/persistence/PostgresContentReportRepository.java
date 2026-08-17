package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ContentReport;
import id.belajarbersama.domain.content.ContentReportRepository;
import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.content.ReportStatus;
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
public class PostgresContentReportRepository implements ContentReportRepository {
    private static final String SELECT =
            """
            SELECT id, content_id, reporter_id, reason, description, status, created_at, updated_at
            FROM content_report
            """;

    private final DataSource dataSource;

    public PostgresContentReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(ContentReport report) {
        String sql =
                """
                INSERT INTO content_report (
                    id, content_id, reporter_id, reason, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, report.id());
            JdbcSupport.setUuid(statement, 2, report.contentId());
            JdbcSupport.setUuid(statement, 3, report.reporterId().value());
            statement.setString(4, report.reason().name());
            statement.setString(5, report.description());
            statement.setString(6, report.status().name());
            JdbcSupport.setInstant(statement, 7, report.createdAt());
            JdbcSupport.setInstant(statement, 8, report.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save report");
        }
    }

    @Override
    public void update(ContentReport report) {
        String sql = "UPDATE content_report SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, report.status().name());
            JdbcSupport.setInstant(statement, 2, report.updatedAt());
            JdbcSupport.setUuid(statement, 3, report.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update report");
        }
    }

    @Override
    public Optional<ContentReport> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load report");
        }
    }

    @Override
    public List<ContentReport> listByContent(UUID contentId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                SELECT + " WHERE content_id = ? ORDER BY created_at DESC")) {
            JdbcSupport.setUuid(statement, 1, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentReport> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list reports");
        }
    }

    @Override
    public List<ContentReport> listOpen() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                SELECT
                                        + " WHERE status IN ('OPEN', 'UNDER_REVIEW') ORDER BY created_at ASC")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentReport> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list open reports");
        }
    }

    @Override
    public boolean hasOpenReport(java.util.UUID reporterId, UUID contentId) {
        String sql =
                """
                SELECT 1 FROM content_report
                WHERE reporter_id = ? AND content_id = ? AND status IN ('OPEN', 'UNDER_REVIEW')
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, reporterId);
            JdbcSupport.setUuid(statement, 2, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to check open report");
        }
    }

    private static ContentReport map(ResultSet resultSet) throws java.sql.SQLException {
        return new ContentReport(
                JdbcSupport.uuid(resultSet, "id"),
                JdbcSupport.uuid(resultSet, "content_id"),
                UserId.of(JdbcSupport.uuid(resultSet, "reporter_id")),
                ReportReason.valueOf(resultSet.getString("reason")),
                resultSet.getString("description"),
                ReportStatus.valueOf(resultSet.getString("status")),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
