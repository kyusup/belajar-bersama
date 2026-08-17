package id.belajarbersama.domain.taxonomy;

import java.time.Instant;
import java.util.UUID;

public record EducationLevel(
        UUID id,
        String slug,
        String name,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
