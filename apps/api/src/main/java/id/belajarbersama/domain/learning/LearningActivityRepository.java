package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LearningActivityRepository {
    void save(UUID id, UserId userId, UUID contentId, LearningActivityKind kind, Instant at);

    Optional<LearningResume> resume(UserId userId);

    void upsertResume(LearningResume resume);
}
