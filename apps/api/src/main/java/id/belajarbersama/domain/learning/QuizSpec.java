package id.belajarbersama.domain.learning;

import java.util.List;
import java.util.UUID;

public record QuizSpec(
        UUID revisionId,
        Integer passingScore,
        Integer maxAttempts,
        boolean required,
        List<QuizQuestion> questions) {}
