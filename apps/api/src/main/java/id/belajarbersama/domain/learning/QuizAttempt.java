package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record QuizAttempt(
        UUID id,
        UserId userId,
        UUID quizId,
        UUID quizRevisionId,
        AttemptStatus status,
        Integer scorePercent,
        Boolean passed,
        Integer correctCount,
        Integer questionCount,
        int version,
        Instant startedAt,
        Instant submittedAt) {
    public boolean ownedBy(UserId actor) {
        return userId.equals(actor);
    }

    public boolean submitted() {
        return status == AttemptStatus.SUBMITTED;
    }
}
