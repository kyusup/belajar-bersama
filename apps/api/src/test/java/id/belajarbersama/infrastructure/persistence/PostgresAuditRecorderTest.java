package id.belajarbersama.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.identity.UserId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PostgresAuditRecorderTest {
    @Inject AuditRecorder auditRecorder;
    @Inject DataSource dataSource;

    @Test
    void persistsAuditEvent() throws Exception {
        UUID target = UUID.randomUUID();
        AuditEvent event =
                AuditEvent.of(
                        UserId.newId(),
                        AuditAction.CONTENT_CREATED,
                        "EducationalContent",
                        target,
                        "corr-1",
                        Map.of("note", "phase-2"));
        auditRecorder.record(event);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT action FROM audit_event WHERE id = ?")) {
            statement.setObject(1, event.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(AuditAction.CONTENT_CREATED.name(), resultSet.getString(1));
            }
        }
    }
}
