package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record ContentReportItemResponse(
        UUID id,
        UUID contentId,
        UUID reporterId,
        String reason,
        String description,
        String status,
        Instant createdAt) {}
