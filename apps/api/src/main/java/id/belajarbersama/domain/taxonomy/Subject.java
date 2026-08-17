package id.belajarbersama.domain.taxonomy;

import java.time.Instant;
import java.util.UUID;

public record Subject(
        UUID id,
        String slug,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
