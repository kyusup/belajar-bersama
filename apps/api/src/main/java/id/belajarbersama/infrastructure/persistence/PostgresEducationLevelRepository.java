package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.taxonomy.EducationLevel;
import id.belajarbersama.domain.taxonomy.EducationLevelRepository;
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
public class PostgresEducationLevelRepository implements EducationLevelRepository {
    private static final String SELECT =
            "SELECT id, slug, name, sort_order, active, created_at, updated_at FROM education_level";

    private final DataSource dataSource;

    public PostgresEducationLevelRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<EducationLevel> findById(UUID id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SELECT + " WHERE id = ?")) {
            JdbcSupport.setUuid(statement, 1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load education level");
        }
    }

    @Override
    public List<EducationLevel> listActive() {
        return list(SELECT + " WHERE active = TRUE ORDER BY sort_order, name");
    }

    @Override
    public List<EducationLevel> listAll() {
        return list(SELECT + " ORDER BY sort_order, name");
    }

    @Override
    public void save(EducationLevel level) {
        String sql =
                """
                INSERT INTO education_level (id, slug, name, sort_order, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, level.id());
            statement.setString(2, level.slug());
            statement.setString(3, level.name());
            statement.setInt(4, level.sortOrder());
            statement.setBoolean(5, level.active());
            JdbcSupport.setInstant(statement, 6, level.createdAt());
            JdbcSupport.setInstant(statement, 7, level.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save education level");
        }
    }

    @Override
    public void update(EducationLevel level) {
        String sql =
                """
                UPDATE education_level SET slug = ?, name = ?, sort_order = ?, active = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, level.slug());
            statement.setString(2, level.name());
            statement.setInt(3, level.sortOrder());
            statement.setBoolean(4, level.active());
            JdbcSupport.setInstant(statement, 5, level.updatedAt());
            JdbcSupport.setUuid(statement, 6, level.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update education level");
        }
    }

    private List<EducationLevel> list(String sql) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<EducationLevel> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list education levels");
        }
    }

    private static EducationLevel map(ResultSet resultSet) throws java.sql.SQLException {
        return new EducationLevel(
                JdbcSupport.uuid(resultSet, "id"),
                resultSet.getString("slug"),
                resultSet.getString("name"),
                resultSet.getInt("sort_order"),
                resultSet.getBoolean("active"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
