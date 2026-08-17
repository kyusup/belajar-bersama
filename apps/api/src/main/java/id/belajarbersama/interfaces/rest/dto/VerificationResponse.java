package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerificationResponse(
        UUID id,
        UUID applicantId,
        UUID competencyId,
        String status,
        String qualification,
        String experience,
        UUID reviewerId,
        String decisionNote,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt,
        List<EvidenceResponse> evidence) {}
