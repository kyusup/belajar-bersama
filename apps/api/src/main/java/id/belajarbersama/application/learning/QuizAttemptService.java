package id.belajarbersama.application.learning;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.AttemptAnswers;
import id.belajarbersama.domain.learning.AttemptStatus;
import id.belajarbersama.domain.learning.LearningActivityKind;
import id.belajarbersama.domain.learning.LearningActivityRepository;
import id.belajarbersama.domain.learning.LearningResume;
import id.belajarbersama.domain.learning.QuizAttempt;
import id.belajarbersama.domain.learning.QuizAttemptRepository;
import id.belajarbersama.domain.learning.QuizOption;
import id.belajarbersama.domain.learning.QuizQuestion;
import id.belajarbersama.domain.learning.QuizScoring;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.domain.learning.QuizSpecRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class QuizAttemptService {
    private final CurrentUserQuery currentUserQuery;
    private final EducationalContentRepository contents;
    private final QuizSpecRepository quizSpecs;
    private final QuizAttemptRepository attempts;
    private final LearningActivityRepository activities;

    public QuizAttemptService(
            CurrentUserQuery currentUserQuery,
            EducationalContentRepository contents,
            QuizSpecRepository quizSpecs,
            QuizAttemptRepository attempts,
            LearningActivityRepository activities) {
        this.currentUserQuery = currentUserQuery;
        this.contents = contents;
        this.quizSpecs = quizSpecs;
        this.attempts = attempts;
        this.activities = activities;
    }

    public EducationalContent requirePublishedQuiz(UUID quizId) {
        EducationalContent content =
                contents.findById(quizId)
                        .orElseThrow(() -> new NotFoundException("Quiz not found."));
        if (content.kind() != ContentKind.QUIZ || !content.publiclyVisible()) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.QUIZ_NOT_PUBLISHED, "Quiz is not published.");
        }
        return content;
    }

    public QuizSpec specForRevision(UUID revisionId) {
        return quizSpecs
                .findByRevision(revisionId)
                .orElseThrow(() -> new NotFoundException("Quiz definition not found."));
    }

    @Transactional
    public QuizAttempt start(UserId actorId, UUID quizId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.LEARNING_PROGRESS_MANAGE);
        EducationalContent quiz = requirePublishedQuiz(quizId);
        QuizSpec spec = specForRevision(quiz.publishedRevisionId());
        QuizAttempt open = attempts.findOpen(actorId, quizId).orElse(null);
        if (open != null) {
            return open;
        }
        int used = attempts.countActive(actorId, quizId);
        if (spec.maxAttempts() != null && used >= spec.maxAttempts()) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.MAX_ATTEMPTS_REACHED, "No remaining quiz attempts.");
        }
        Instant now = Instant.now();
        QuizAttempt attempt =
                new QuizAttempt(
                        UUID.randomUUID(),
                        actorId,
                        quiz.id(),
                        quiz.publishedRevisionId(),
                        AttemptStatus.IN_PROGRESS,
                        null,
                        null,
                        null,
                        spec.questions().size(),
                        0,
                        now,
                        null);
        try {
            attempts.save(attempt);
        } catch (RuntimeException exception) {
            return attempts.findOpen(actorId, quizId)
                    .orElseThrow(
                            () ->
                                    new ConflictException(
                                            ErrorCodes.CONCURRENT_MODIFICATION,
                                            "Could not start the attempt."));
        }
        activities.save(
                UUID.randomUUID(), actorId, quiz.id(), LearningActivityKind.QUIZ_STARTED, now);
        activities.upsertResume(new LearningResume(actorId, quiz.id(), nearestCourse(quiz), now));
        return attempt;
    }

    @Transactional
    public QuizAttempt saveAnswers(UserId actorId, UUID attemptId, Map<UUID, Set<UUID>> answers) {
        QuizAttempt attempt = requireOwnedAttempt(actorId, attemptId);
        if (attempt.status() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.ATTEMPT_ALREADY_SUBMITTED, "Submitted attempts cannot be changed.");
        }
        QuizSpec spec = specForRevision(attempt.quizRevisionId());
        AttemptAnswers cleaned = validateAnswers(spec, answers);
        attempts.replaceAnswers(attempt.id(), cleaned);
        return attempt;
    }

    @Transactional
    public QuizAttempt submit(UserId actorId, UUID attemptId, Map<UUID, Set<UUID>> answers) {
        QuizAttempt attempt = requireOwnedAttempt(actorId, attemptId);
        if (attempt.status() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.ATTEMPT_ALREADY_SUBMITTED, "This attempt is already submitted.");
        }
        QuizSpec spec = specForRevision(attempt.quizRevisionId());
        AttemptAnswers cleaned = validateAnswers(spec, answers == null ? Map.of() : answers);
        attempts.replaceAnswers(attempt.id(), cleaned);
        QuizScoring.ScoreResult score =
                QuizScoring.score(spec.questions(), cleaned.selectedByQuestion());
        Boolean passed = QuizScoring.passed(spec.passingScore(), score.percent());
        Instant now = Instant.now();
        QuizAttempt submitted =
                new QuizAttempt(
                        attempt.id(),
                        attempt.userId(),
                        attempt.quizId(),
                        attempt.quizRevisionId(),
                        AttemptStatus.SUBMITTED,
                        score.percent(),
                        passed,
                        score.correctCount(),
                        score.questionCount(),
                        attempt.version(),
                        attempt.startedAt(),
                        now);
        if (!attempts.updateIfVersion(submitted, attempt.version())) {
            throw new ConflictException(
                    ErrorCodes.CONCURRENT_MODIFICATION, "Attempt was submitted concurrently.");
        }
        activities.save(
                UUID.randomUUID(),
                actorId,
                attempt.quizId(),
                LearningActivityKind.QUIZ_SUBMITTED,
                now);
        return attempts.findById(attempt.id()).orElse(submitted);
    }

    public QuizAttempt requireOwnedAttempt(UserId actorId, UUID attemptId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QuizAttempt attempt =
                attempts.findById(attemptId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCodes.ATTEMPT_NOT_FOUND,
                                                "Attempt not found."));
        if (!attempt.ownedBy(actorId)) {
            throw new NotFoundException(ErrorCodes.ATTEMPT_NOT_FOUND, "Attempt not found.");
        }
        return attempt;
    }

    public AttemptAnswers answers(UUID attemptId) {
        return attempts.answers(attemptId);
    }

    public List<QuizAttempt> history(UserId actorId, UUID quizId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.QUIZ_HISTORY_READ);
        return attempts.listByUserAndQuiz(actorId, quizId);
    }

    public List<QuizAttempt> recent(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.QUIZ_HISTORY_READ);
        return attempts.listRecentSubmitted(actorId, 10);
    }

    private AttemptAnswers validateAnswers(QuizSpec spec, Map<UUID, Set<UUID>> answers) {
        Map<UUID, Set<UUID>> allowed = new HashMap<>();
        for (QuizQuestion question : spec.questions()) {
            Set<UUID> optionIds = new HashSet<>();
            for (QuizOption option : question.options()) {
                optionIds.add(option.id());
            }
            Set<UUID> selected = answers.getOrDefault(question.id(), Set.of());
            for (UUID optionId : selected) {
                if (!optionIds.contains(optionId)) {
                    throw new BusinessRuleViolationException(
                            ErrorCodes.INVALID_QUESTION_ANSWER,
                            "Answer does not belong to this question.");
                }
            }
            if (question.type() != id.belajarbersama.domain.learning.QuestionType.MULTIPLE_CHOICE
                    && selected.size() > 1) {
                throw new BusinessRuleViolationException(
                        ErrorCodes.INVALID_QUESTION_ANSWER, "Only one option is allowed.");
            }
            allowed.put(question.id(), Set.copyOf(selected));
        }
        for (UUID questionId : answers.keySet()) {
            if (!allowed.containsKey(questionId)) {
                throw new BusinessRuleViolationException(
                        ErrorCodes.INVALID_QUESTION_ANSWER,
                        "Unknown question in this quiz revision.");
            }
        }
        return new AttemptAnswers(allowed);
    }

    private UUID nearestCourse(EducationalContent content) {
        EducationalContent cursor = content;
        while (cursor != null) {
            if (cursor.kind() == ContentKind.COURSE) {
                return cursor.id();
            }
            if (cursor.parentId() == null) {
                return null;
            }
            cursor = contents.findById(cursor.parentId()).orElse(null);
        }
        return null;
    }
}
