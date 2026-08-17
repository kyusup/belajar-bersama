package id.belajarbersama.application.content;

import id.belajarbersama.domain.content.ContentBody;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.ContentSource;
import id.belajarbersama.domain.content.LicenseCode;
import id.belajarbersama.domain.learning.QuestionDifficulty;
import id.belajarbersama.domain.learning.QuestionType;
import java.util.List;
import java.util.UUID;

public record ContentDraftInput(
        ContentKind kind,
        String title,
        String summary,
        UUID subjectId,
        UUID educationLevelId,
        UUID parentId,
        List<UUID> competencyIds,
        LicenseCode license,
        ContentBody body,
        List<ContentSource> sources,
        String changeSummary,
        int sortOrder,
        boolean required,
        QuizDraft quiz) {
    public record QuizDraft(
            Integer passingScore,
            Integer maxAttempts,
            Boolean required,
            List<QuestionDraft> questions) {}

    public record QuestionDraft(
            UUID id,
            String type,
            String prompt,
            String explanation,
            String difficulty,
            UUID competencyId,
            String reference,
            List<OptionDraft> options) {}

    public record OptionDraft(UUID id, String label, String text, boolean correct) {}

    public static QuestionType parseType(String type) {
        return QuestionType.valueOf(type.trim().toUpperCase());
    }

    public static QuestionDifficulty parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return QuestionDifficulty.MEDIUM;
        }
        return QuestionDifficulty.valueOf(difficulty.trim().toUpperCase());
    }
}
