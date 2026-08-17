package id.belajarbersama.domain.qa;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record QaQuestion(
        UUID id,
        UserId authorId,
        String title,
        String body,
        UUID subjectId,
        UUID contentId,
        QaStatus status,
        UUID acceptedAnswerId,
        Instant createdAt,
        Instant updatedAt) {
    public boolean authoredBy(UserId userId) {
        return authorId.equals(userId);
    }

    public boolean publiclyVisible() {
        return status != QaStatus.HIDDEN;
    }

    public boolean acceptsAnswers() {
        return status == QaStatus.OPEN;
    }
}
