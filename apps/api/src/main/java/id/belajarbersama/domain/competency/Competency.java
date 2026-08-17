package id.belajarbersama.domain.competency;

import java.time.Instant;
import java.util.UUID;

public record Competency(
        UUID id,
        String slug,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
