package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.verification.Verification;
import id.belajarbersama.domain.verification.VerificationEvidence;
import id.belajarbersama.domain.verification.VerificationRepository;
import id.belajarbersama.domain.verification.VerificationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresVerificationRepository implements VerificationRepository {
    private static final String SELECT =
            """
            SELECT id, user_id, competency_id, status, qualification, experience,
                   reviewer_id, decision_note, decided_at, created_at, updated_at
            FROM verification
            """;

    private final DataSource dataSource;

    public PostgresVerificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Verification verification) {
        String sql =
                """
                INSERT INTO verification (
                    id, user_id, competency_id, status, qualification, experience,
                    reviewer_id, decision_note, decided_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, verification);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save verification");
        }
    }

    @Override
    public void update(Verification verification) {
        String sql =
                """
                UPDATE verification SET
                    status = ?, qualification = ?, experience = ?, reviewer_id = ?,
                    decision_note = ?, decided_at = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, verification.status().name());
            statement.setString(2, verification.qualification());
            statement.setString(3, verification.experience());
            JdbcSupport.setUuid(
                    statement,
                    4,
                    verification.reviewerId() == null ? null : verification.reviewerId().value());
            statement.setString(5, verification.decisionNote());
            JdbcSupport.setInstant(statement, 6, verification.decidedAt());
            JdbcSupport.setInstant(statement, 7, verification.updatedAt());
            JdbcSupport.setUuid(statement, 8, verification.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update verification");
        }
    }

    @Override
    public Optional<Verification> findById(UUID id) {
        return queryOne(SELECT + " WHERE id = ?", id);
    }

    @Override
    public List<Verification> listByApplicant(UserId applicantId) {
        return queryList(
                SELECT + " WHERE user_id = ? ORDER BY created_at DESC", applicantId.value());
    }

    @Override
    public List<Verification> listByStatus(VerificationStatus status) {
        String sql = SELECT + " WHERE status = ? ORDER BY created_at";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            return readList(statement);
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list verifications");
        }
    }

    @Override
    public Optional<Verification> findApproved(UserId userId, UUID competencyId) {
        String sql = SELECT + " WHERE user_id = ? AND competency_id = ? AND status = 'APPROVED'";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, competencyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load approved verification");
        }
    }

    @Override
    public Set<UUID> approvedCompetencyIds(UserId userId) {
        String sql =
                "SELECT competency_id FROM verification WHERE user_id = ? AND status = 'APPROVED'";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<UUID> ids = new HashSet<>();
                while (resultSet.next()) {
                    ids.add(JdbcSupport.uuid(resultSet, "competency_id"));
                }
                return ids;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load approved competencies");
        }
    }

    @Override
    public boolean hasOpenApplication(UserId userId, UUID competencyId) {
        String sql =
                """
                SELECT 1 FROM verification
                WHERE user_id = ? AND competency_id = ?
                  AND status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CHANGES_REQUESTED')
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            JdbcSupport.setUuid(statement, 2, competencyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to check open verification");
        }
    }

    @Override
    public void saveEvidence(VerificationEvidence evidence) {
        String sql =
                """
                INSERT INTO verification_evidence (
                    id, verification_id, kind, summary, reference_url, storage_key, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, evidence.id());
            JdbcSupport.setUuid(statement, 2, evidence.verificationId());
            statement.setString(3, evidence.kind());
            statement.setString(4, evidence.summary());
            statement.setString(5, evidence.referenceUrl());
            statement.setString(6, evidence.storageKey());
            JdbcSupport.setInstant(statement, 7, evidence.createdAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save evidence");
        }
    }

    @Override
    public List<VerificationEvidence> listEvidence(UUID verificationId) {
        String sql =
                """
                SELECT id, verification_id, kind, summary, reference_url, storage_key, created_at
                FROM verification_evidence WHERE verification_id = ? ORDER BY created_at
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, verificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<VerificationEvidence> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(
                            new VerificationEvidence(
                                    JdbcSupport.uuid(resultSet, "id"),
                                    JdbcSupport.uuid(resultSet, "verification_id"),
                                    resultSet.getString("kind"),
                                    resultSet.getString("summary"),
                                    resultSet.getString("reference_url"),
                                    resultSet.getString("storage_key"),
                                    JdbcSupport.instant(resultSet, "created_at")));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list evidence");
        }
    }

    private void bind(PreparedStatement statement, Verification verification)
            throws java.sql.SQLException {
        JdbcSupport.setUuid(statement, 1, verification.id());
        JdbcSupport.setUuid(statement, 2, verification.applicantId().value());
        JdbcSupport.setUuid(statement, 3, verification.competencyId());
        statement.setString(4, verification.status().name());
        statement.setString(5, verification.qualification());
        statement.setString(6, verification.experience());
        JdbcSupport.setUuid(
                statement,
                7,
                verification.reviewerId() == null ? null : verification.reviewerId().value());
        statement.setString(8, verification.decisionNote());
        JdbcSupport.setInstant(statement, 9, verification.decidedAt());
        JdbcSupport.setInstant(statement, 10, verification.createdAt());
        JdbcSupport.setInstant(statement, 11, verification.updatedAt());
    }

    private Optional<Verification> queryOne(String sql, UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load verification");
        }
    }

    private List<Verification> queryList(String sql, UUID userId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId);
            return readList(statement);
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list verifications");
        }
    }

    private static List<Verification> readList(PreparedStatement statement)
            throws java.sql.SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Verification> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        }
    }

    private static Verification map(ResultSet resultSet) throws java.sql.SQLException {
        UUID reviewer = JdbcSupport.uuid(resultSet, "reviewer_id");
        return new Verification(
                JdbcSupport.uuid(resultSet, "id"),
                UserId.of(JdbcSupport.uuid(resultSet, "user_id")),
                JdbcSupport.uuid(resultSet, "competency_id"),
                VerificationStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("qualification"),
                resultSet.getString("experience"),
                reviewer == null ? null : UserId.of(reviewer),
                resultSet.getString("decision_note"),
                JdbcSupport.instant(resultSet, "decided_at"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
