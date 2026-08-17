package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.content.ReportStatus;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.qa.QaReport;
import id.belajarbersama.domain.qa.QaReportRepository;
import id.belajarbersama.domain.qa.QaTargetType;
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
public class PostgresQaReportRepository implements QaReportRepository {
    private static final String SELECT =
            """
            SELECT id, reporter_id, target_type, target_id, reason, description, status,
                   created_at, updated_at
            FROM qa_report
            """;

    private final DataSource dataSource;

    public PostgresQaReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(QaReport report) {
        String sql =
                """
                INSERT INTO qa_report (
                    id, reporter_id, target_type, target_id, reason, description, status,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, report.id());
            JdbcSupport.setUuid(statement, 2, report.reporterId().value());
            statement.setString(3, report.targetType().name());
            JdbcSupport.setUuid(statement, 4, report.targetId());
            statement.setString(5, report.reason().name());
            statement.setString(6, report.description());
            statement.setString(7, report.status().name());
            JdbcSupport.setInstant(statement, 8, report.createdAt());
            JdbcSupport.setInstant(statement, 9, report.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save Q&A report");
        }
    }

    @Override
    public void update(QaReport report) {
        String sql = "UPDATE qa_report SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, report.status().name());
            JdbcSupport.setInstant(statement, 2, report.updatedAt());
            JdbcSupport.setUuid(statement, 3, report.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update Q&A report");
        }
    }

    @Override
    public Optional<QaReport> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load Q&A report");
        }
    }

    @Override
    public boolean hasOpenReport(UUID reporterId, QaTargetType type, UUID targetId) {
        String sql =
                """
                SELECT 1 FROM qa_report
                WHERE reporter_id = ? AND target_type = ? AND target_id = ?
                  AND status IN ('OPEN', 'UNDER_REVIEW')
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, reporterId);
            statement.setString(2, type.name());
            JdbcSupport.setUuid(statement, 3, targetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to check open Q&A report");
        }
    }

    @Override
    public List<QaReport> listOpen() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                SELECT
                                        + " WHERE status IN ('OPEN', 'UNDER_REVIEW') ORDER BY created_at ASC")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<QaReport> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list Q&A reports");
        }
    }

    private static QaReport map(ResultSet resultSet) throws java.sql.SQLException {
        return new QaReport(
                JdbcSupport.uuid(resultSet, "id"),
                UserId.of(JdbcSupport.uuid(resultSet, "reporter_id")),
                QaTargetType.valueOf(resultSet.getString("target_type")),
                JdbcSupport.uuid(resultSet, "target_id"),
                ReportReason.valueOf(resultSet.getString("reason")),
                resultSet.getString("description"),
                ReportStatus.valueOf(resultSet.getString("status")),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
