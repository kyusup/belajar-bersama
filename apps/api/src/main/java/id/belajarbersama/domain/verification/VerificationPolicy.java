package id.belajarbersama.domain.verification;

import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.Set;

public final class VerificationPolicy {
    private static final Set<VerificationStatus> REVIEWABLE =
            Set.of(
                    VerificationStatus.SUBMITTED,
                    VerificationStatus.UNDER_REVIEW,
                    VerificationStatus.CHANGES_REQUESTED);

    private VerificationPolicy() {}

    public static void assertNotSelfReview(UserId applicant, UserId reviewer) {
        if (applicant == null || reviewer == null) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.VALIDATION_FAILED, "Applicant and reviewer are required.");
        }
        if (applicant.equals(reviewer)) {
            throw new AuthorizationException(
                    ErrorCodes.CANNOT_VERIFY_SELF, "A user cannot verify themselves.");
        }
    }

    public static Verification startReview(Verification current, UserId reviewer, Instant at) {
        assertNotSelfReview(current.applicantId(), reviewer);
        assertReviewable(current);
        return copy(
                current,
                VerificationStatus.UNDER_REVIEW,
                reviewer,
                current.decisionNote(),
                null,
                at);
    }

    public static Verification approve(
            Verification current, UserId reviewer, String note, Instant at) {
        assertNotSelfReview(current.applicantId(), reviewer);
        assertReviewable(current);
        return copy(current, VerificationStatus.APPROVED, reviewer, note, at, at);
    }

    public static Verification reject(
            Verification current, UserId reviewer, String note, Instant at) {
        assertNotSelfReview(current.applicantId(), reviewer);
        assertReviewable(current);
        return copy(current, VerificationStatus.REJECTED, reviewer, note, at, at);
    }

    public static Verification requestChanges(
            Verification current, UserId reviewer, String note, Instant at) {
        assertNotSelfReview(current.applicantId(), reviewer);
        assertReviewable(current);
        return copy(current, VerificationStatus.CHANGES_REQUESTED, reviewer, note, at, at);
    }

    public static Verification revoke(
            Verification current, UserId reviewer, String note, Instant at) {
        assertNotSelfReview(current.applicantId(), reviewer);
        if (current.status() != VerificationStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_VERIFICATION_TRANSITION,
                    "Only an approved verification can be revoked.");
        }
        return copy(current, VerificationStatus.REVOKED, reviewer, note, at, at);
    }

    public static Verification resubmit(Verification current, Instant at) {
        if (current.status() != VerificationStatus.CHANGES_REQUESTED
                && current.status() != VerificationStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_VERIFICATION_TRANSITION,
                    "Verification cannot be submitted in its current state.");
        }
        return copy(current, VerificationStatus.SUBMITTED, current.reviewerId(), null, null, at);
    }

    private static void assertReviewable(Verification current) {
        if (!REVIEWABLE.contains(current.status())) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_VERIFICATION_TRANSITION,
                    "Verification cannot be reviewed in its current state.");
        }
    }

    private static Verification copy(
            Verification current,
            VerificationStatus status,
            UserId reviewer,
            String note,
            Instant decidedAt,
            Instant updatedAt) {
        return new Verification(
                current.id(),
                current.applicantId(),
                current.competencyId(),
                status,
                current.qualification(),
                current.experience(),
                reviewer,
                note,
                decidedAt,
                current.createdAt(),
                updatedAt);
    }
}
