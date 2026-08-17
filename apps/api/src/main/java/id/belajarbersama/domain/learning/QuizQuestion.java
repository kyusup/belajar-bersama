package id.belajarbersama.domain.learning;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record QuizQuestion(
        UUID id,
        UUID revisionId,
        int sortOrder,
        QuestionType type,
        String prompt,
        String explanation,
        QuestionDifficulty difficulty,
        UUID competencyId,
        String reference,
        List<QuizOption> options) {
    public Set<UUID> correctOptionIds() {
        return options.stream()
                .filter(QuizOption::correct)
                .map(QuizOption::id)
                .collect(Collectors.toSet());
    }
}
