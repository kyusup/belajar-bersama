package id.belajarbersama.domain.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuizScoringTest {
    @Test
    void singleChoiceRequiresExactOption() {
        UUID questionId = UUID.randomUUID();
        UUID correct = UUID.randomUUID();
        UUID wrong = UUID.randomUUID();
        QuizQuestion question =
                question(
                        questionId,
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                option(wrong, questionId, false),
                                option(correct, questionId, true)));
        assertTrue(QuizScoring.questionCorrect(question, Set.of(correct)));
        assertFalse(QuizScoring.questionCorrect(question, Set.of(wrong)));
        assertFalse(QuizScoring.questionCorrect(question, Set.of()));
        assertFalse(QuizScoring.questionCorrect(question, Set.of(correct, wrong)));
    }

    @Test
    void multipleChoiceRequiresExactSetNoPartialCredit() {
        UUID questionId = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        QuizQuestion question =
                question(
                        questionId,
                        QuestionType.MULTIPLE_CHOICE,
                        List.of(
                                option(a, questionId, true),
                                option(b, questionId, true),
                                option(c, questionId, false)));
        assertTrue(QuizScoring.questionCorrect(question, Set.of(a, b)));
        assertFalse(QuizScoring.questionCorrect(question, Set.of(a)));
        assertFalse(QuizScoring.questionCorrect(question, Set.of(a, b, c)));
    }

    @Test
    void percentIsCorrectOverTotalAndPassingScoreIsOptional() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID firstCorrect = UUID.randomUUID();
        UUID secondCorrect = UUID.randomUUID();
        List<QuizQuestion> questions =
                List.of(
                        question(
                                firstId,
                                QuestionType.SINGLE_CHOICE,
                                List.of(
                                        option(firstCorrect, firstId, true),
                                        option(UUID.randomUUID(), firstId, false))),
                        question(
                                secondId,
                                QuestionType.TRUE_FALSE,
                                List.of(
                                        option(secondCorrect, secondId, true),
                                        option(UUID.randomUUID(), secondId, false))));
        QuizScoring.ScoreResult half =
                QuizScoring.score(questions, Map.of(firstId, Set.of(firstCorrect)));
        assertEquals(1, half.correctCount());
        assertEquals(2, half.questionCount());
        assertEquals(50, half.percent());
        assertTrue(QuizScoring.passed(50, half.percent()));
        assertFalse(QuizScoring.passed(70, half.percent()));
        assertNull(QuizScoring.passed(null, half.percent()));
    }

    private static QuizQuestion question(UUID id, QuestionType type, List<QuizOption> options) {
        return new QuizQuestion(
                id,
                UUID.randomUUID(),
                0,
                type,
                "prompt",
                "explanation",
                QuestionDifficulty.EASY,
                null,
                null,
                options);
    }

    private static QuizOption option(UUID id, UUID questionId, boolean correct) {
        return new QuizOption(id, questionId, 0, "A", "text", correct);
    }
}
