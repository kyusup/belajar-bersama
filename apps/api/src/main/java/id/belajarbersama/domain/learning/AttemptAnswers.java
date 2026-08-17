package id.belajarbersama.domain.learning;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AttemptAnswers(Map<UUID, Set<UUID>> selectedByQuestion) {
    public Set<UUID> selected(UUID questionId) {
        return selectedByQuestion.getOrDefault(questionId, Set.of());
    }
}
