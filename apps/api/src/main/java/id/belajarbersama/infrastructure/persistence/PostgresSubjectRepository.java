package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.taxonomy.Subject;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
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
public class PostgresSubjectRepository implements SubjectRepository {
    private static final String SELECT =
            "SELECT id, slug, name, description, active, created_at, updated_at FROM subject";

    private final DataSource dataSource;

    public PostgresSubjectRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Subject> findById(UUID id) {
        return one(SELECT + " WHERE id = ?", id);
    }

    @Override
    public Optional<Subject> findBySlug(String slug) {
        String sql = SELECT + " WHERE slug = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load subject");
        }
    }

    @Override
    public List<Subject> listActive() {
        return list(SELECT + " WHERE active = TRUE ORDER BY name");
    }

    @Override
    public List<Subject> listAll() {
        return list(SELECT + " ORDER BY name");
    }

    @Override
    public void save(Subject subject) {
        String sql =
                """
                INSERT INTO subject (id, slug, name, description, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        write(sql, subject, false);
    }

    @Override
    public void update(Subject subject) {
        String sql =
                """
                UPDATE subject SET slug = ?, name = ?, description = ?, active = ?, updated_at = ?
                WHERE id = ?
                """;
        write(sql, subject, true);
    }

    private void write(String sql, Subject subject, boolean update) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (update) {
                statement.setString(1, subject.slug());
                statement.setString(2, subject.name());
                statement.setString(3, subject.description());
                statement.setBoolean(4, subject.active());
                JdbcSupport.setInstant(statement, 5, subject.updatedAt());
                JdbcSupport.setUuid(statement, 6, subject.id());
            } else {
                JdbcSupport.setUuid(statement, 1, subject.id());
                statement.setString(2, subject.slug());
                statement.setString(3, subject.name());
                statement.setString(4, subject.description());
                statement.setBoolean(5, subject.active());
                JdbcSupport.setInstant(statement, 6, subject.createdAt());
                JdbcSupport.setInstant(statement, 7, subject.updatedAt());
            }
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save subject");
        }
    }

    private Optional<Subject> one(String sql, UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load subject");
        }
    }

    private List<Subject> list(String sql) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<Subject> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list subjects");
        }
    }

    private static Subject map(ResultSet resultSet) throws java.sql.SQLException {
        return new Subject(
                JdbcSupport.uuid(resultSet, "id"),
                resultSet.getString("slug"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBoolean("active"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
