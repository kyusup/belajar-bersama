package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record ContentSubmission(
        UUID id,
        UUID contentId,
        UUID revisionId,
        UserId makerId,
        SubmissionStatus status,
        UserId assignedCheckerId,
        UserId assignedBy,
        Instant assignedAt,
        int version,
        Instant createdAt,
        Instant updatedAt) {}
