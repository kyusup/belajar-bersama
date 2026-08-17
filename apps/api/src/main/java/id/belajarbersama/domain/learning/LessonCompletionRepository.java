package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LessonCompletionRepository {
    boolean complete(LessonCompletion completion);

    boolean exists(UserId userId, UUID contentId);

    Set<UUID> completedContentIds(UserId userId, List<UUID> contentIds);

    List<LessonCompletion> listByUser(UserId userId);
}
