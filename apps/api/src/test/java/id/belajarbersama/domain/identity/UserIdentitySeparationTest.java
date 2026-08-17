package id.belajarbersama.domain.identity;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserIdentitySeparationTest {
    @Test
    void identitySubjectIsNotUserId() {
        UserId userId = UserId.newId();
        String googleSubject = "google-subject-abc";
        Identity identity =
                new Identity(
                        UUID.randomUUID(),
                        userId,
                        IdentityProviderId.GOOGLE,
                        "https://accounts.google.com",
                        googleSubject,
                        Instant.now());
        assertNotEquals(userId.value().toString(), identity.subject());
        assertNotEquals(IdentityProviderId.GOOGLE.name(), userId.value().toString());
    }
}
