package id.belajarbersama.interfaces.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AttemptResponse(
        UUID id,
        UUID quizId,
        UUID quizRevisionId,
        String status,
        Integer scorePercent,
        Boolean passed,
        Integer correctCount,
        Integer questionCount,
        Instant startedAt,
        Instant submittedAt,
        Map<UUID, List<UUID>> answers,
        List<ReviewQuestion> review) {
    public record ReviewQuestion(
            UUID id,
            String prompt,
            String type,
            String explanation,
            Set<UUID> correctOptionIds,
            Set<UUID> selectedOptionIds,
            boolean correct,
            List<PublicQuizResponse.Option> options) {}
}
