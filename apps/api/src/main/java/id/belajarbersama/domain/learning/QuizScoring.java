package id.belajarbersama.domain.learning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuizScoring {
    private QuizScoring() {}

    public static boolean questionCorrect(QuizQuestion question, Set<UUID> selected) {
        Set<UUID> chosen = selected == null ? Set.of() : selected;
        return question.correctOptionIds().equals(chosen);
    }

    public static ScoreResult score(List<QuizQuestion> questions, Map<UUID, Set<UUID>> answers) {
        int total = questions.size();
        int correct = 0;
        for (QuizQuestion question : questions) {
            if (questionCorrect(question, answers.getOrDefault(question.id(), Set.of()))) {
                correct++;
            }
        }
        int percent = total == 0 ? 0 : (correct * 100) / total;
        return new ScoreResult(correct, total, percent);
    }

    public static Boolean passed(Integer passingScore, int percent) {
        if (passingScore == null) {
            return null;
        }
        return percent >= passingScore;
    }

    public record ScoreResult(int correctCount, int questionCount, int percent) {}
}
