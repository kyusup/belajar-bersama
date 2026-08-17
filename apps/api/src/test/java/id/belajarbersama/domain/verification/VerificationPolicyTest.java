package id.belajarbersama.domain.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationPolicyTest {
    @Test
    void applicantCannotApproveOwnVerification() {
        UserId applicant = UserId.newId();
        Verification submitted = sample(applicant, VerificationStatus.SUBMITTED);
        assertThrows(
                AuthorizationException.class,
                () -> VerificationPolicy.approve(submitted, applicant, "self", Instant.now()));
    }

    @Test
    void approveRecordsReviewer() {
        UserId applicant = UserId.newId();
        UserId admin = UserId.newId();
        Verification approved =
                VerificationPolicy.approve(
                        sample(applicant, VerificationStatus.SUBMITTED),
                        admin,
                        "memadai",
                        Instant.now());
        assertEquals(VerificationStatus.APPROVED, approved.status());
        assertEquals(admin, approved.reviewerId());
    }

    private static Verification sample(UserId applicant, VerificationStatus status) {
        Instant now = Instant.now();
        return new Verification(
                UUID.randomUUID(),
                applicant,
                UUID.fromString("aaaaaaaa-0001-4000-8000-000000000001"),
                status,
                "S1",
                "mengajar",
                null,
                null,
                null,
                now,
                now);
    }
}
