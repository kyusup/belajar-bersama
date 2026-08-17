package id.belajarbersama.domain.verification;

import java.time.Instant;
import java.util.UUID;

public record VerificationEvidence(
        UUID id,
        UUID verificationId,
        String kind,
        String summary,
        String referenceUrl,
        String storageKey,
        Instant createdAt) {}
