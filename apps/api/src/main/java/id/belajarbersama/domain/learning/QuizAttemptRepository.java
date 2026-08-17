package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository {
    void save(QuizAttempt attempt);

    boolean updateIfVersion(QuizAttempt attempt, int expectedVersion);

    Optional<QuizAttempt> findById(UUID id);

    Optional<QuizAttempt> findOpen(UserId userId, UUID quizId);

    List<QuizAttempt> listByUserAndQuiz(UserId userId, UUID quizId);

    List<QuizAttempt> listRecentSubmitted(UserId userId, int limit);

    int countActive(UserId userId, UUID quizId);

    boolean hasPassingSubmission(UserId userId, UUID quizId);

    boolean hasSubmittedAttempt(UserId userId, UUID quizId);

    AttemptAnswers answers(UUID attemptId);

    void replaceAnswers(UUID attemptId, AttemptAnswers answers);
}
