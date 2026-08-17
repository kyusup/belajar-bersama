package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record ContentReport(
        UUID id,
        UUID contentId,
        UserId reporterId,
        ReportReason reason,
        String description,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt) {}
