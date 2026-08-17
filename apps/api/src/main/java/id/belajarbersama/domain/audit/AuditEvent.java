package id.belajarbersama.domain.audit;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        Instant occurredAt,
        UserId actorUserId,
        AuditAction action,
        String targetType,
        UUID targetId,
        String correlationId,
        Map<String, Object> metadata) {
    public static AuditEvent of(
            UserId actorUserId,
            AuditAction action,
            String targetType,
            UUID targetId,
            String correlationId,
            Map<String, Object> metadata) {
        return new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                actorUserId,
                action,
                targetType,
                targetId,
                correlationId,
                metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
