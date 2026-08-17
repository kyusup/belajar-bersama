package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.competency.Competency;
import id.belajarbersama.domain.competency.CompetencyRepository;
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
public class PostgresCompetencyRepository implements CompetencyRepository {
    private static final String SELECT =
            """
            SELECT id, slug, name, description, active, created_at, updated_at
            FROM competency
            """;

    private final DataSource dataSource;

    public PostgresCompetencyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Competency> findById(UUID id) {
        String sql = SELECT + " WHERE id = ?";
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
            throw JdbcSupport.wrap(exception, "Failed to load competency");
        }
    }

    @Override
    public List<Competency> listActive() {
        return list(SELECT + " WHERE active = TRUE ORDER BY name");
    }

    @Override
    public List<Competency> listAll() {
        return list(SELECT + " ORDER BY name");
    }

    @Override
    public void save(Competency competency) {
        String sql =
                """
                INSERT INTO competency (id, slug, name, description, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, competency.id());
            statement.setString(2, competency.slug());
            statement.setString(3, competency.name());
            statement.setString(4, competency.description());
            statement.setBoolean(5, competency.active());
            JdbcSupport.setInstant(statement, 6, competency.createdAt());
            JdbcSupport.setInstant(statement, 7, competency.updatedAt());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to save competency");
        }
    }

    @Override
    public void update(Competency competency) {
        String sql =
                """
                UPDATE competency SET slug = ?, name = ?, description = ?, active = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, competency.slug());
            statement.setString(2, competency.name());
            statement.setString(3, competency.description());
            statement.setBoolean(4, competency.active());
            JdbcSupport.setInstant(statement, 5, competency.updatedAt());
            JdbcSupport.setUuid(statement, 6, competency.id());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to update competency");
        }
    }

    private List<Competency> list(String sql) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<Competency> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(map(resultSet));
            }
            return items;
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to list competencies");
        }
    }

    private static Competency map(ResultSet resultSet) throws java.sql.SQLException {
        return new Competency(
                JdbcSupport.uuid(resultSet, "id"),
                resultSet.getString("slug"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getBoolean("active"),
                JdbcSupport.instant(resultSet, "created_at"),
                JdbcSupport.instant(resultSet, "updated_at"));
    }
}
