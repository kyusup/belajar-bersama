package id.belajarbersama.interfaces.rest.dto;

import java.util.List;
import java.util.UUID;

public record PublicQuizResponse(
        UUID id,
        String slug,
        String title,
        String summary,
        Integer passingScore,
        Integer maxAttempts,
        boolean required,
        UUID revisionId,
        int revisionNumber,
        List<Question> questions) {
    public record Question(
            UUID id,
            String type,
            String prompt,
            String difficulty,
            String reference,
            List<Option> options) {}

    public record Option(UUID id, String label, String text) {}
}
