package id.belajarbersama.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.error.InfrastructureException;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresAuditRecorder implements AuditRecorder {
    private static final String INSERT =
            """
            INSERT INTO audit_event (
                id, occurred_at, actor_user_id, action, target_type, target_id, correlation_id, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public PostgresAuditRecorder(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(AuditEvent event) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setObject(1, event.id());
            statement.setTimestamp(2, Timestamp.from(event.occurredAt()));
            if (event.actorUserId() == null) {
                statement.setObject(3, null);
            } else {
                statement.setObject(3, event.actorUserId().value());
            }
            statement.setString(4, event.action().name());
            statement.setString(5, event.targetType());
            statement.setObject(6, event.targetId());
            statement.setString(7, event.correlationId());
            statement.setString(8, toJson(event));
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to persist audit event", exception);
        }
    }

    private String toJson(AuditEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event.metadata());
    }
}
