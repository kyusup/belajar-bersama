package id.belajarbersama.interfaces.rest.dto;

import id.belajarbersama.domain.content.ContentBody;
import java.util.List;
import java.util.UUID;

public record CreateContentRequest(
        String kind,
        String title,
        String summary,
        UUID subjectId,
        UUID educationLevelId,
        UUID parentId,
        List<UUID> competencyIds,
        String license,
        ContentBody body,
        List<ContentSourceRequest> sources,
        String changeSummary,
        Integer sortOrder,
        Boolean required,
        QuizRequest quiz) {
    public record ContentSourceRequest(
            String title,
            String author,
            String publisher,
            String url,
            String publicationInfo,
            String notes) {}

    public record QuizRequest(
            Integer passingScore,
            Integer maxAttempts,
            Boolean required,
            List<QuestionRequest> questions) {}

    public record QuestionRequest(
            UUID id,
            String type,
            String prompt,
            String explanation,
            String difficulty,
            UUID competencyId,
            String reference,
            List<OptionRequest> options) {}

    public record OptionRequest(UUID id, String label, String text, boolean correct) {}
}
