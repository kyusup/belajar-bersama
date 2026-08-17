package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.ContentStatus;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.identity.UserId;
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
public class PostgresEducationalContentRepository implements EducationalContentRepository {
    private static final String SELECT =
            """
            SELECT id, kind, slug, maker_id, subject_id, education_level_id, parent_id, status,
                   current_revision_id, published_revision_id, archived_at, sort_order, required,
                   version, created_at, updated_at
            FROM educational_content
            """;

    private final DataSource dataSource;

    public PostgresEducationalContentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(EducationalContent content) {
        String sql =
                """
                INSERT INTO educational_content (
                    id, kind, slug, maker_id, subject_id, education_level_id, parent_id, status,
                    current_revision_id, published_revision_id, archived_at, sort_order, required,
                    version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, content);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save content");
        }
    }

    @Override
    public boolean update(EducationalContent content) {
        String sql =
                """
                UPDATE educational_content SET
                    kind = ?, slug = ?, subject_id = ?, education_level_id = ?, parent_id = ?,
                    status = ?, current_revision_id = ?, published_revision_id = ?, archived_at = ?,
                    sort_order = ?, required = ?, version = ?, updated_at = ?
                WHERE id = ? AND version = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content.kind().name());
            statement.setString(2, content.slug());
            JdbcSupport.setUuid(statement, 3, content.subjectId());
            JdbcSupport.setUuid(statement, 4, content.educationLevelId());
            JdbcSupport.setUuid(statement, 5, content.parentId());
            statement.setString(6, content.status().name());
            JdbcSupport.setUuid(statement, 7, content.currentRevisionId());
            JdbcSupport.setUuid(statement, 8, content.publishedRevisionId());
            JdbcSupport.setInstant(statement, 9, content.archivedAt());
            statement.setInt(10, content.sortOrder());
            statement.setBoolean(11, content.required());
            statement.setInt(12, content.version() + 1);
            JdbcSupport.setInstant(statement, 13, content.updatedAt());
            JdbcSupport.setUuid(statement, 14, content.id());
            statement.setInt(15, content.version());
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update content");
        }
    }

    @Override
    public Optional<EducationalContent> findById(UUID id) {
        return one(SELECT + " WHERE id = ?", statement -> JdbcSupport.setUuid(statement, 1, id));
    }

    @Override
    public Optional<EducationalContent> findBySlug(String slug) {
        return one(SELECT + " WHERE slug = ?", statement -> statement.setString(1, slug));
    }

    @Override
    public Optional<UUID> contentIdForSlugHistory(String slug) {
        String sql = "SELECT content_id FROM content_slug_history WHERE slug = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(JdbcSupport.uuid(resultSet, "content_id"))
                        : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load slug history");
        }
    }

    @Override
    public List<EducationalContent> listByMaker(UserId makerId) {
        return list(
                SELECT + " WHERE maker_id = ? ORDER BY updated_at DESC",
                statement -> JdbcSupport.setUuid(statement, 1, makerId.value()));
    }

    @Override
    public List<EducationalContent> listPublicBySubject(UUID subjectId) {
        return list(
                SELECT
                        + " WHERE subject_id = ? AND published_revision_id IS NOT NULL AND archived_at IS NULL"
                        + " ORDER BY updated_at DESC",
                statement -> JdbcSupport.setUuid(statement, 1, subjectId));
    }

    @Override
    public List<EducationalContent> listPublic() {
        return listPublic(null, null);
    }

    @Override
    public List<EducationalContent> listPublic(ContentKind kind, UUID subjectId) {
        StringBuilder sql =
                new StringBuilder(
                        SELECT
                                + " WHERE published_revision_id IS NOT NULL AND archived_at IS NULL");
        if (kind != null) {
            sql.append(" AND kind = ?");
        }
        if (subjectId != null) {
            sql.append(" AND subject_id = ?");
        }
        sql.append(" ORDER BY sort_order, updated_at DESC");
        return list(
                sql.toString(),
                statement -> {
                    int index = 1;
                    if (kind != null) {
                        statement.setString(index++, kind.name());
                    }
                    if (subjectId != null) {
                        JdbcSupport.setUuid(statement, index, subjectId);
                    }
                });
    }

    @Override
    public List<EducationalContent> listPublicChildren(UUID parentId) {
        return list(
                SELECT
                        + " WHERE parent_id = ? AND published_revision_id IS NOT NULL AND archived_at IS NULL"
                        + " ORDER BY sort_order, slug",
                statement -> JdbcSupport.setUuid(statement, 1, parentId));
    }

    @Override
    public List<EducationalContent> listPublishedDescendants(UUID rootId) {
        String sql =
                """
                WITH RECURSIVE tree AS (
                    SELECT * FROM educational_content WHERE id = ?
                    UNION ALL
                    SELECT child.* FROM educational_content child
                    JOIN tree ON child.parent_id = tree.id
                )
                SELECT id, kind, slug, maker_id, subject_id, education_level_id, parent_id, status,
                       current_revision_id, published_revision_id, archived_at, sort_order, required,
                       version, created_at, updated_at
                FROM tree
                WHERE published_revision_id IS NOT NULL AND archived_at IS NULL
                ORDER BY sort_order, slug
                """;
        return list(sql, statement -> JdbcSupport.setUuid(statement, 1, rootId));
    }

    @Override
    public boolean slugTaken(String slug) {
        String sql =
                "SELECT 1 FROM educational_content WHERE slug = ? UNION SELECT 1 FROM content_slug_history WHERE slug = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            statement.setString(2, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to check slug");
        }
    }

    @Override
    public void saveSlugHistory(String slug, UUID contentId) {
        String sql =
                """
                INSERT INTO content_slug_history (slug, content_id, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (slug) DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            JdbcSupport.setUuid(statement, 2, contentId);
            JdbcSupport.setInstant(statement, 3, Instant.now());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save slug history");
        }
    }

    private void bind(PreparedStatement statement, EducationalContent content) throws Exception {
        JdbcSupport.setUuid(statement, 1, content.id());
        statement.setString(2, content.kind().name());
        statement.setString(3, content.slug());
        JdbcSupport.setUuid(statement, 4, content.makerId().value());
        JdbcSupport.setUuid(statement, 5, content.subjectId());
        JdbcSupport.setUuid(statement, 6, content.educationLevelId());
        JdbcSupport.setUuid(statement, 7, content.parentId());
        statement.setString(8, content.status().name());
        JdbcSupport.setUuid(statement, 9, content.currentRevisionId());
        JdbcSupport.setUuid(statement, 10, content.publishedRevisionId());
        JdbcSupport.setInstant(statement, 11, content.archivedAt());
        statement.setInt(12, content.sortOrder());
        statement.setBoolean(13, content.required());
        statement.setInt(14, content.version());
        JdbcSupport.setInstant(statement, 15, content.createdAt());
        JdbcSupport.setInstant(statement, 16, content.updatedAt());
    }

    private Optional<EducationalContent> one(String sql, Binder binder) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load content");
        }
    }

    private List<EducationalContent> list(String sql, Binder binder) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EducationalContent> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(map(resultSet));
                }
                return items;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list content");
        }
    }

    private static EducationalContent map(ResultSet resultSet) throws java.sql.SQLException {
        return new EducationalContent(
                JdbcSupport.uuid(resultSet, "id"),
                ContentKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("slug"),
                UserId.of(JdbcSupport.uuid(resultSet, "maker_id")),
                JdbcSupport.uuid(resultSet, "subject_id"),
                JdbcSupport.uuid(resultSet, "education_level_id"),
                JdbcSupport.uuid(resultSet, "parent_id"),
                ContentStatus.valueOf(resultSet.getString("status")),
                JdbcSupport.uuid(resultSet, "current_revision_id"),
                JdbcSupport.uuid(resultSet, "published_revision_id"),
                JdbcSupport.instant(resultSet, "archived_at"),
                resultSet.getInt("sort_order"),
                resultSet.getBoolean("required"),
                resultSet.getInt("version"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
