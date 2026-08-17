package id.belajarbersama.domain.verification;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record Verification(
        UUID id,
        UserId applicantId,
        UUID competencyId,
        VerificationStatus status,
        String qualification,
        String experience,
        UserId reviewerId,
        String decisionNote,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt) {
    public boolean isApproved() {
        return status == VerificationStatus.APPROVED;
    }

    public boolean isOpen() {
        return status == VerificationStatus.DRAFT
                || status == VerificationStatus.SUBMITTED
                || status == VerificationStatus.UNDER_REVIEW
                || status == VerificationStatus.CHANGES_REQUESTED;
    }
}
