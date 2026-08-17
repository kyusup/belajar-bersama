package id.belajarbersama.infrastructure.persistence;

import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.identity.RoleAssignmentRepository;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresRoleAssignmentRepository implements RoleAssignmentRepository {
    private final DataSource dataSource;

    public PostgresRoleAssignmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void assign(UserId userId, Role role, UserId assignedBy, Instant at) {
        String sql =
                """
                INSERT INTO user_role (user_id, role_id, assigned_by, assigned_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            statement.setString(2, role.name());
            JdbcSupport.setUuid(statement, 3, assignedBy == null ? null : assignedBy.value());
            JdbcSupport.setInstant(statement, 4, at);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to assign role");
        }
    }

    @Override
    public void revoke(UserId userId, Role role) {
        String sql = "DELETE FROM user_role WHERE user_id = ? AND role_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            statement.setString(2, role.name());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to revoke role");
        }
    }

    @Override
    public Set<Role> rolesOf(UserId userId) {
        String sql = "SELECT role_id FROM user_role WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcSupport.setUuid(statement, 1, userId.value());
            try (ResultSet resultSet = statement.executeQuery()) {
                EnumSet<Role> roles = EnumSet.noneOf(Role.class);
                while (resultSet.next()) {
                    roles.add(Role.valueOf(resultSet.getString(1)));
                }
                return roles;
            }
        } catch (Exception exception) {
            throw JdbcSupport.wrap(exception, "Failed to load roles");
        }
    }

    @Override
    public boolean hasRole(UserId userId, Role role) {
        return rolesOf(userId).contains(role);
    }
}
