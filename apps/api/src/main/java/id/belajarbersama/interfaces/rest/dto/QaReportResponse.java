package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record QaReportResponse(
        UUID id,
        String targetType,
        UUID targetId,
        String reason,
        String description,
        String status,
        Instant createdAt) {}
