package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.error.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuizDraftFactory {
    private QuizDraftFactory() {}

    public static QuizSpec fromDraft(UUID revisionId, QuizDraftInput input) {
        if (input == null) {
            throw new ValidationException("Quiz definition is required.");
        }
        if (input.passingScore() != null
                && (input.passingScore() < 0 || input.passingScore() > 100)) {
            throw new ValidationException("Passing score must be between 0 and 100.");
        }
        if (input.maxAttempts() != null && input.maxAttempts() < 1) {
            throw new ValidationException("maxAttempts must be at least 1.");
        }
        if (input.questions() == null || input.questions().isEmpty()) {
            throw new ValidationException("A quiz needs at least one question.");
        }
        List<QuizQuestion> questions = new ArrayList<>();
        int order = 0;
        for (QuestionInput question : input.questions()) {
            questions.add(question(revisionId, question, order++));
        }
        return new QuizSpec(
                revisionId,
                input.passingScore(),
                input.maxAttempts(),
                input.required() == null || input.required(),
                List.copyOf(questions));
    }

    public static void assertReady(QuizSpec spec) {
        if (spec == null || spec.questions() == null || spec.questions().isEmpty()) {
            throw new ValidationException("A quiz needs at least one question.");
        }
        for (QuizQuestion question : spec.questions()) {
            validateQuestion(question);
        }
    }

    private static QuizQuestion question(UUID revisionId, QuestionInput input, int order) {
        if (input == null
                || input.prompt() == null
                || ContentSanitizer.plainText(input.prompt()).isBlank()) {
            throw new ValidationException("Question prompt is required.");
        }
        QuestionType type;
        try {
            type = QuestionType.valueOf(input.type().trim().toUpperCase());
        } catch (Exception exception) {
            throw new ValidationException("Unknown question type.");
        }
        QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;
        if (input.difficulty() != null && !input.difficulty().isBlank()) {
            try {
                difficulty = QuestionDifficulty.valueOf(input.difficulty().trim().toUpperCase());
            } catch (Exception exception) {
                throw new ValidationException("Unknown question difficulty.");
            }
        }
        if (input.options() == null || input.options().size() < 2) {
            throw new ValidationException("Each question needs at least two options.");
        }
        List<QuizOption> options = new ArrayList<>();
        int optionOrder = 0;
        UUID questionId = input.id() == null ? UUID.randomUUID() : input.id();
        for (OptionInput option : input.options()) {
            String text = ContentSanitizer.plainText(option == null ? null : option.text());
            if (text.isBlank()) {
                throw new ValidationException("Option text is required.");
            }
            String label =
                    ContentSanitizer.plainText(
                            option.label() == null
                                    ? String.valueOf((char) ('A' + optionOrder))
                                    : option.label());
            options.add(
                    new QuizOption(
                            option.id() == null ? UUID.randomUUID() : option.id(),
                            questionId,
                            optionOrder++,
                            label.isBlank()
                                    ? String.valueOf((char) ('A' + optionOrder - 1))
                                    : label,
                            text,
                            option.correct()));
        }
        QuizQuestion question =
                new QuizQuestion(
                        questionId,
                        revisionId,
                        order,
                        type,
                        ContentSanitizer.plainText(input.prompt()),
                        ContentSanitizer.plainText(input.explanation()),
                        difficulty,
                        input.competencyId(),
                        ContentSanitizer.plainText(input.reference()),
                        List.copyOf(options));
        validateQuestion(question);
        return question;
    }

    private static void validateQuestion(QuizQuestion question) {
        long correct = question.options().stream().filter(QuizOption::correct).count();
        if (question.type() == QuestionType.MULTIPLE_CHOICE) {
            if (correct < 1) {
                throw new ValidationException(
                        "Multiple-choice questions need at least one correct option.");
            }
        } else if (correct != 1) {
            throw new ValidationException(
                    "This question type requires exactly one correct option.");
        }
        if (question.type() == QuestionType.TRUE_FALSE && question.options().size() != 2) {
            throw new ValidationException("True/false questions require exactly two options.");
        }
    }

    public record QuizDraftInput(
            Integer passingScore,
            Integer maxAttempts,
            Boolean required,
            List<QuestionInput> questions) {}

    public record QuestionInput(
            UUID id,
            String type,
            String prompt,
            String explanation,
            String difficulty,
            UUID competencyId,
            String reference,
            List<OptionInput> options) {}

    public record OptionInput(UUID id, String label, String text, boolean correct) {}
}
