package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ContentRevision;
import id.belajarbersama.domain.content.ContentRevisionRepository;
import id.belajarbersama.domain.content.ContentSource;
import id.belajarbersama.domain.content.LicenseCode;
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
public class PostgresContentRevisionRepository implements ContentRevisionRepository {
    private static final String SELECT =
            """
            SELECT id, content_id, revision_number, title, summary, body::text AS body, license_code,
                   change_summary, created_by, created_at
            FROM content_revision
            """;

    private final DataSource dataSource;

    public PostgresContentRevisionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(ContentRevision revision) {
        String sql =
                """
                INSERT INTO content_revision (
                    id, content_id, revision_number, title, summary, body, license_code,
                    change_summary, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, revision.id());
            JdbcSupport.setUuid(statement, 2, revision.contentId());
            statement.setInt(3, revision.revisionNumber());
            statement.setString(4, revision.title());
            statement.setString(5, revision.summary());
            statement.setString(6, ContentBodyJson.write(revision.body()));
            statement.setString(7, revision.license().name());
            statement.setString(8, revision.changeSummary());
            JdbcSupport.setUuid(statement, 9, revision.createdBy().value());
            JdbcSupport.setInstant(statement, 10, revision.createdAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save revision");
        }
        replaceCompetencies(revision);
        replaceSources(revision);
    }

    @Override
    public void updateMutable(ContentRevision revision) {
        String sql =
                """
                UPDATE content_revision SET title = ?, summary = ?, body = ?::jsonb,
                    license_code = ?, change_summary = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, revision.title());
            statement.setString(2, revision.summary());
            statement.setString(3, ContentBodyJson.write(revision.body()));
            statement.setString(4, revision.license().name());
            statement.setString(5, revision.changeSummary());
            JdbcSupport.setUuid(statement, 6, revision.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update revision");
        }
        replaceCompetencies(revision);
        replaceSources(revision);
    }

    @Override
    public Optional<ContentRevision> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load revision");
        }
    }

    @Override
    public List<ContentRevision> listByContent(UUID contentId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                SELECT + " WHERE content_id = ? ORDER BY revision_number")) {
            JdbcSupport.setUuid(statement, 1, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentRevision> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list revisions");
        }
    }

    @Override
    public int nextRevisionNumber(UUID contentId) {
        String sql =
                "SELECT COALESCE(MAX(revision_number), 0) + 1 FROM content_revision WHERE content_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, contentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to allocate revision number");
        }
    }

    private void replaceCompetencies(ContentRevision revision) {
        String delete = "DELETE FROM content_revision_competency WHERE revision_id = ?";
        String insert =
                "INSERT INTO content_revision_competency (revision_id, competency_id) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                JdbcSupport.setUuid(statement, 1, revision.id());
                statement.executeUpdate();
            }
            if (revision.competencyIds() == null) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                for (UUID competencyId : revision.competencyIds()) {
                    JdbcSupport.setUuid(statement, 1, revision.id());
                    JdbcSupport.setUuid(statement, 2, competencyId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save revision competencies");
        }
    }

    private void replaceSources(ContentRevision revision) {
        String delete = "DELETE FROM content_source WHERE revision_id = ?";
        String insert =
                """
                INSERT INTO content_source (
                    id, revision_id, title, author, publisher, url, publication_info, notes, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                JdbcSupport.setUuid(statement, 1, revision.id());
                statement.executeUpdate();
            }
            if (revision.sources() == null) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                int order = 0;
                for (ContentSource source : revision.sources()) {
                    JdbcSupport.setUuid(statement, 1, source.id());
                    JdbcSupport.setUuid(statement, 2, revision.id());
                    statement.setString(3, source.title());
                    statement.setString(4, source.author());
                    statement.setString(5, source.publisher());
                    statement.setString(6, source.url());
                    statement.setString(7, source.publicationInfo());
                    statement.setString(8, source.notes());
                    statement.setInt(9, source.sortOrder() == 0 ? order : source.sortOrder());
                    statement.addBatch();
                    order++;
                }
                statement.executeBatch();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save sources");
        }
    }

    private ContentRevision map(ResultSet resultSet) throws Exception {
        UUID id = JdbcSupport.uuid(resultSet, "id");
        return new ContentRevision(
                id,
                JdbcSupport.uuid(resultSet, "content_id"),
                resultSet.getInt("revision_number"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                ContentBodyJson.read(resultSet.getString("body")),
                LicenseCode.valueOf(resultSet.getString("license_code")),
                resultSet.getString("change_summary"),
                UserId.of(JdbcSupport.uuid(resultSet, "created_by")),
                JdbcSupport.instant(resultSet, "created_at"),
                loadCompetencies(id),
                loadSources(id));
    }

    private List<UUID> loadCompetencies(UUID revisionId) {
        String sql = "SELECT competency_id FROM content_revision_competency WHERE revision_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, revisionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<UUID> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(JdbcSupport.uuid(resultSet, "competency_id"));
                }
                return ids;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load revision competencies");
        }
    }

    private List<ContentSource> loadSources(UUID revisionId) {
        String sql =
                """
                SELECT id, title, author, publisher, url, publication_info, notes, sort_order
                FROM content_source WHERE revision_id = ? ORDER BY sort_order
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, revisionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentSource> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(
                            new ContentSource(
                                    JdbcSupport.uuid(resultSet, "id"),
                                    resultSet.getString("title"),
                                    resultSet.getString("author"),
                                    resultSet.getString("publisher"),
                                    resultSet.getString("url"),
                                    resultSet.getString("publication_info"),
                                    resultSet.getString("notes"),
                                    resultSet.getInt("sort_order")));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load sources");
        }
    }
}
