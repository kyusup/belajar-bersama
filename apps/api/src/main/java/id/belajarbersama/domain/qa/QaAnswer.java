package id.belajarbersama.domain.qa;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record QaAnswer(
        UUID id,
        UUID questionId,
        UserId authorId,
        String body,
        boolean hidden,
        Instant createdAt,
        Instant updatedAt) {
    public boolean authoredBy(UserId userId) {
        return authorId.equals(userId);
    }

    public boolean publiclyVisible() {
        return !hidden;
    }
}
