package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record ContentReview(
        UUID id,
        UUID submissionId,
        UUID revisionId,
        UserId reviewerId,
        ReviewDecision decision,
        String comment,
        Instant createdAt,
        Instant decidedAt) {}
