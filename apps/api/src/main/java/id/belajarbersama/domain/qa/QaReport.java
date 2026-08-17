package id.belajarbersama.domain.qa;

import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.content.ReportStatus;
import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record QaReport(
        UUID id,
        UserId reporterId,
        QaTargetType targetType,
        UUID targetId,
        ReportReason reason,
        String description,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt) {}
