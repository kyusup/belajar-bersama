package id.belajarbersama.application.qa;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.content.ReportStatus;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.qa.QaAnswer;
import id.belajarbersama.domain.qa.QaAnswerRepository;
import id.belajarbersama.domain.qa.QaQuestion;
import id.belajarbersama.domain.qa.QaQuestionRepository;
import id.belajarbersama.domain.qa.QaReport;
import id.belajarbersama.domain.qa.QaReportRepository;
import id.belajarbersama.domain.qa.QaStatus;
import id.belajarbersama.domain.qa.QaTargetType;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class QaService {
    private static final int TITLE_MAX = 200;
    private static final int BODY_MAX = 8_000;

    private final CurrentUserQuery currentUserQuery;
    private final QaQuestionRepository questions;
    private final QaAnswerRepository answers;
    private final QaReportRepository reports;
    private final EducationalContentRepository contents;
    private final SubjectRepository subjects;
    private final UserRepository users;
    private final AuditRecorder auditRecorder;

    public QaService(
            CurrentUserQuery currentUserQuery,
            QaQuestionRepository questions,
            QaAnswerRepository answers,
            QaReportRepository reports,
            EducationalContentRepository contents,
            SubjectRepository subjects,
            UserRepository users,
            AuditRecorder auditRecorder) {
        this.currentUserQuery = currentUserQuery;
        this.questions = questions;
        this.answers = answers;
        this.reports = reports;
        this.contents = contents;
        this.subjects = subjects;
        this.users = users;
        this.auditRecorder = auditRecorder;
    }

    public List<QaQuestion> listPublic(UUID contentId, UUID subjectId, int page, int size) {
        return questions.listPublic(
                contentId, subjectId, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    public long countPublic(UUID contentId, UUID subjectId) {
        return questions.countPublic(contentId, subjectId);
    }

    public QaQuestion requirePublic(UUID id) {
        QaQuestion question =
                questions
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCodes.QA_NOT_FOUND, "Question not found."));
        if (!question.publiclyVisible()) {
            throw new NotFoundException(ErrorCodes.QA_NOT_FOUND, "Question not found.");
        }
        return question;
    }

    public QaQuestion requireKnown(UUID id) {
        return questions
                .findById(id)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCodes.QA_NOT_FOUND, "Question not found."));
    }

    public List<QaAnswer> answers(UUID questionId, boolean includeHidden) {
        List<QaAnswer> items = answers.listByQuestion(questionId, includeHidden);
        UUID accepted =
                questions.findById(questionId).map(QaQuestion::acceptedAnswerId).orElse(null);
        if (accepted == null) {
            return items;
        }
        return items.stream()
                .sorted(
                        (left, right) -> {
                            if (left.id().equals(accepted)) {
                                return -1;
                            }
                            if (right.id().equals(accepted)) {
                                return 1;
                            }
                            return 0;
                        })
                .toList();
    }

    public int usefulCount(UUID answerId) {
        return answers.usefulCount(answerId);
    }

    public boolean markedUseful(UserId userId, UUID answerId) {
        return userId != null && answers.markedUseful(userId, answerId);
    }

    public String displayName(UserId userId) {
        return users.findById(userId).map(User::displayName).orElse("Peserta");
    }

    @Transactional
    public QaQuestion ask(
            UserId actorId,
            String title,
            String body,
            UUID subjectId,
            UUID contentId,
            String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.QA_ASK);
        String cleanTitle = requireText(title, TITLE_MAX, "Title");
        String cleanBody = requireText(body, BODY_MAX, "Body");
        UUID subject = optionalSubject(subjectId);
        UUID content = optionalPublishedContent(contentId);
        Instant now = Instant.now();
        QaQuestion question =
                new QaQuestion(
                        UUID.randomUUID(),
                        actorId,
                        cleanTitle,
                        cleanBody,
                        subject,
                        content,
                        QaStatus.OPEN,
                        null,
                        now,
                        now);
        questions.save(question);
        audit(actorId, AuditAction.QA_QUESTION_CREATED, question.id(), correlationId, Map.of());
        return question;
    }

    @Transactional
    public QaQuestion updateQuestion(UserId actorId, UUID questionId, String title, String body) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QaQuestion question = requireKnown(questionId);
        if (!question.authoredBy(actorId)) {
            throw new NotFoundException(ErrorCodes.QA_NOT_FOUND, "Question not found.");
        }
        if (question.status() == QaStatus.HIDDEN) {
            throw new NotFoundException(ErrorCodes.QA_NOT_FOUND, "Question not found.");
        }
        QaQuestion updated =
                new QaQuestion(
                        question.id(),
                        question.authorId(),
                        requireText(title, TITLE_MAX, "Title"),
                        requireText(body, BODY_MAX, "Body"),
                        question.subjectId(),
                        question.contentId(),
                        question.status(),
                        question.acceptedAnswerId(),
                        question.createdAt(),
                        Instant.now());
        questions.update(updated);
        return updated;
    }

    @Transactional
    public QaQuestion close(UserId actorId, UUID questionId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QaQuestion question = requirePublic(questionId);
        boolean moderator = actor.permissions().contains(Permission.CONTENT_MODERATE);
        if (!question.authoredBy(actorId) && !moderator) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.QA_NOT_AUTHOR,
                    "Only the asker or a moderator can close this question.");
        }
        QaQuestion updated =
                new QaQuestion(
                        question.id(),
                        question.authorId(),
                        question.title(),
                        question.body(),
                        question.subjectId(),
                        question.contentId(),
                        QaStatus.CLOSED,
                        question.acceptedAnswerId(),
                        question.createdAt(),
                        Instant.now());
        questions.update(updated);
        audit(actorId, AuditAction.QA_QUESTION_CLOSED, question.id(), correlationId, Map.of());
        return updated;
    }

    @Transactional
    public QaAnswer answer(UserId actorId, UUID questionId, String body, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.QA_ANSWER);
        QaQuestion question = requirePublic(questionId);
        if (!question.acceptsAnswers()) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.QA_CLOSED, "This question is closed.");
        }
        Instant now = Instant.now();
        QaAnswer answer =
                new QaAnswer(
                        UUID.randomUUID(),
                        question.id(),
                        actorId,
                        requireText(body, BODY_MAX, "Body"),
                        false,
                        now,
                        now);
        answers.save(answer);
        audit(
                actorId,
                AuditAction.QA_ANSWER_CREATED,
                answer.id(),
                correlationId,
                Map.of("questionId", question.id().toString()));
        return answer;
    }

    @Transactional
    public QaAnswer updateAnswer(UserId actorId, UUID answerId, String body) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QaAnswer answer = requireAnswer(answerId);
        if (!answer.authoredBy(actorId) || answer.hidden()) {
            throw new NotFoundException(ErrorCodes.ANSWER_NOT_FOUND, "Answer not found.");
        }
        QaAnswer updated =
                new QaAnswer(
                        answer.id(),
                        answer.questionId(),
                        answer.authorId(),
                        requireText(body, BODY_MAX, "Body"),
                        false,
                        answer.createdAt(),
                        Instant.now());
        answers.update(updated);
        return updated;
    }

    @Transactional
    public QaQuestion accept(UserId actorId, UUID questionId, UUID answerId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QaQuestion question = requirePublic(questionId);
        boolean moderator = actor.permissions().contains(Permission.CONTENT_MODERATE);
        if (!question.authoredBy(actorId) && !moderator) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.QA_NOT_AUTHOR,
                    "Only the asker or a moderator can accept an answer.");
        }
        QaAnswer answer = requireAnswer(answerId);
        if (!answer.questionId().equals(questionId) || answer.hidden()) {
            throw new NotFoundException(ErrorCodes.ANSWER_NOT_FOUND, "Answer not found.");
        }
        QaQuestion updated =
                new QaQuestion(
                        question.id(),
                        question.authorId(),
                        question.title(),
                        question.body(),
                        question.subjectId(),
                        question.contentId(),
                        question.status(),
                        answer.id(),
                        question.createdAt(),
                        Instant.now());
        questions.update(updated);
        audit(
                actorId,
                AuditAction.QA_ANSWER_ACCEPTED,
                question.id(),
                correlationId,
                Map.of("answerId", answer.id().toString()));
        return updated;
    }

    @Transactional
    public QaQuestion unaccept(UserId actorId, UUID questionId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        QaQuestion question = requirePublic(questionId);
        boolean moderator = actor.permissions().contains(Permission.CONTENT_MODERATE);
        if (!question.authoredBy(actorId) && !moderator) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.QA_NOT_AUTHOR,
                    "Only the asker or a moderator can change the accepted answer.");
        }
        QaQuestion updated =
                new QaQuestion(
                        question.id(),
                        question.authorId(),
                        question.title(),
                        question.body(),
                        question.subjectId(),
                        question.contentId(),
                        question.status(),
                        null,
                        question.createdAt(),
                        Instant.now());
        questions.update(updated);
        return updated;
    }

    @Transactional
    public void markUseful(UserId actorId, UUID answerId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.QA_MARK_USEFUL);
        QaAnswer answer = requireVisibleAnswer(answerId);
        if (answer.authoredBy(actorId)) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CANNOT_MARK_OWN_ANSWER, "You cannot mark your own answer useful.");
        }
        answers.addUseful(actorId, answer.id());
    }

    @Transactional
    public void unmarkUseful(UserId actorId, UUID answerId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        answers.removeUseful(actorId, answerId);
    }

    @Transactional
    public QaReport report(
            UserId actorId,
            QaTargetType type,
            UUID targetId,
            ReportReason reason,
            String description,
            String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.CONTENT_REPORT);
        if (type == QaTargetType.QUESTION) {
            requirePublic(targetId);
        } else {
            requireVisibleAnswer(targetId);
        }
        if (reason == null) {
            throw new ValidationException("Report reason is required.");
        }
        String text = requireText(description, BODY_MAX, "Description");
        if (reports.hasOpenReport(actorId.value(), type, targetId)) {
            throw new ConflictException(ErrorCodes.CONFLICT, "An open report already exists.");
        }
        Instant now = Instant.now();
        QaReport report =
                new QaReport(
                        UUID.randomUUID(),
                        actorId,
                        type,
                        targetId,
                        reason,
                        text,
                        ReportStatus.OPEN,
                        now,
                        now);
        reports.save(report);
        audit(
                actorId,
                AuditAction.QA_REPORTED,
                report.id(),
                correlationId,
                Map.of("targetType", type.name(), "targetId", targetId.toString()));
        return report;
    }

    @Transactional
    public QaQuestion hideQuestion(UserId actorId, UUID questionId, String correlationId) {
        requireModerator(actorId);
        QaQuestion question = requireKnown(questionId);
        QaQuestion updated =
                new QaQuestion(
                        question.id(),
                        question.authorId(),
                        question.title(),
                        question.body(),
                        question.subjectId(),
                        question.contentId(),
                        QaStatus.HIDDEN,
                        question.acceptedAnswerId(),
                        question.createdAt(),
                        Instant.now());
        questions.update(updated);
        audit(
                actorId,
                AuditAction.QA_HIDDEN,
                question.id(),
                correlationId,
                Map.of("target", "QUESTION"));
        return updated;
    }

    @Transactional
    public QaAnswer hideAnswer(UserId actorId, UUID answerId, String correlationId) {
        requireModerator(actorId);
        QaAnswer answer = requireAnswer(answerId);
        QaAnswer updated =
                new QaAnswer(
                        answer.id(),
                        answer.questionId(),
                        answer.authorId(),
                        answer.body(),
                        true,
                        answer.createdAt(),
                        Instant.now());
        answers.update(updated);
        questions
                .findById(answer.questionId())
                .filter(question -> answer.id().equals(question.acceptedAnswerId()))
                .ifPresent(
                        question ->
                                questions.update(
                                        new QaQuestion(
                                                question.id(),
                                                question.authorId(),
                                                question.title(),
                                                question.body(),
                                                question.subjectId(),
                                                question.contentId(),
                                                question.status(),
                                                null,
                                                question.createdAt(),
                                                Instant.now())));
        audit(
                actorId,
                AuditAction.QA_HIDDEN,
                answer.id(),
                correlationId,
                Map.of("target", "ANSWER"));
        return updated;
    }

    public QaQuestion requireForModeration(UserId actorId, UUID id) {
        requireModerator(actorId);
        return requireKnown(id);
    }

    public List<QaReport> openReports(UserId actorId) {
        requireModerator(actorId);
        return reports.listOpen();
    }

    @Transactional
    public QaReport resolveReport(
            UserId actorId, UUID reportId, ReportStatus status, String correlationId) {
        requireModerator(actorId);
        if (status != ReportStatus.RESOLVED && status != ReportStatus.DISMISSED) {
            throw new ValidationException("Report can only be resolved or dismissed.");
        }
        QaReport report =
                reports.findById(reportId)
                        .orElseThrow(() -> new NotFoundException("Report not found."));
        QaReport updated =
                new QaReport(
                        report.id(),
                        report.reporterId(),
                        report.targetType(),
                        report.targetId(),
                        report.reason(),
                        report.description(),
                        status,
                        report.createdAt(),
                        Instant.now());
        reports.update(updated);
        audit(
                actorId,
                status == ReportStatus.RESOLVED
                        ? AuditAction.CONTENT_REPORT_RESOLVED
                        : AuditAction.CONTENT_REPORT_DISMISSED,
                report.id(),
                correlationId,
                Map.of("kind", "QA"));
        return updated;
    }

    private void requireModerator(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.CONTENT_MODERATE);
    }

    private QaAnswer requireAnswer(UUID answerId) {
        return answers.findById(answerId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCodes.ANSWER_NOT_FOUND, "Answer not found."));
    }

    private QaAnswer requireVisibleAnswer(UUID answerId) {
        QaAnswer answer = requireAnswer(answerId);
        if (answer.hidden()) {
            throw new NotFoundException(ErrorCodes.ANSWER_NOT_FOUND, "Answer not found.");
        }
        QaQuestion question = requirePublic(answer.questionId());
        if (!question.publiclyVisible()) {
            throw new NotFoundException(ErrorCodes.ANSWER_NOT_FOUND, "Answer not found.");
        }
        return answer;
    }

    private UUID optionalSubject(UUID subjectId) {
        if (subjectId == null) {
            return null;
        }
        return subjects.findById(subjectId)
                .map(item -> item.id())
                .orElseThrow(() -> new ValidationException("Unknown subject."));
    }

    private UUID optionalPublishedContent(UUID contentId) {
        if (contentId == null) {
            return null;
        }
        var content =
                contents.findById(contentId)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.publiclyVisible()) {
            throw new NotFoundException("Content not found.");
        }
        return content.id();
    }

    private static String requireText(String raw, int max, String field) {
        String text = ContentSanitizer.plainText(raw);
        if (text.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        if (text.length() > max) {
            throw new ValidationException(field + " is too long.");
        }
        return text;
    }

    private void audit(
            UserId actor,
            AuditAction action,
            UUID target,
            String correlationId,
            Map<String, Object> metadata) {
        auditRecorder.record(AuditEvent.of(actor, action, "Qa", target, correlationId, metadata));
    }
}
