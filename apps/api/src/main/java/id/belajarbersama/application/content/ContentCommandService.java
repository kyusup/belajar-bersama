package id.belajarbersama.application.content;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.competency.CompetencyRepository;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.ContentLifecycle;
import id.belajarbersama.domain.content.ContentRevision;
import id.belajarbersama.domain.content.ContentRevisionRepository;
import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.content.ContentSource;
import id.belajarbersama.domain.content.ContentStatus;
import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.ContentSubmissionRepository;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.content.LicenseCode;
import id.belajarbersama.domain.content.Slugs;
import id.belajarbersama.domain.content.SubmissionStatus;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.QuizDraftFactory;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.domain.learning.QuizSpecRepository;
import id.belajarbersama.domain.search.SearchDocument;
import id.belajarbersama.domain.search.SearchIndex;
import id.belajarbersama.domain.taxonomy.EducationLevelRepository;
import id.belajarbersama.domain.taxonomy.Subject;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ContentCommandService {
    private final CurrentUserQuery currentUserQuery;
    private final EducationalContentRepository contents;
    private final ContentRevisionRepository revisions;
    private final ContentSubmissionRepository submissions;
    private final SubjectRepository subjects;
    private final EducationLevelRepository levels;
    private final CompetencyRepository competencies;
    private final QuizSpecRepository quizSpecs;
    private final AuditRecorder auditRecorder;
    private final SearchIndex searchIndex;

    public ContentCommandService(
            CurrentUserQuery currentUserQuery,
            EducationalContentRepository contents,
            ContentRevisionRepository revisions,
            ContentSubmissionRepository submissions,
            SubjectRepository subjects,
            EducationLevelRepository levels,
            CompetencyRepository competencies,
            QuizSpecRepository quizSpecs,
            AuditRecorder auditRecorder,
            SearchIndex searchIndex) {
        this.currentUserQuery = currentUserQuery;
        this.contents = contents;
        this.revisions = revisions;
        this.submissions = submissions;
        this.subjects = subjects;
        this.levels = levels;
        this.competencies = competencies;
        this.quizSpecs = quizSpecs;
        this.auditRecorder = auditRecorder;
        this.searchIndex = searchIndex;
    }

    @Transactional
    public EducationalContent create(
            UserId actorId, ContentDraftInput input, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        ContentDraftInput sanitized = sanitize(input);
        AuthorizationPolicies.assertCanCreateContent(
                actor.user(),
                actor.permissions(),
                actor.approvedCompetencyIds(),
                Set.copyOf(sanitized.competencyIds()));
        Instant now = Instant.now();
        UUID contentId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        String slug = uniqueSlug(Slugs.fromTitle(sanitized.title()));
        ContentRevision revision =
                new ContentRevision(
                        revisionId,
                        contentId,
                        1,
                        sanitized.title(),
                        sanitized.summary(),
                        sanitized.body(),
                        sanitized.license(),
                        sanitized.changeSummary(),
                        actorId,
                        now,
                        sanitized.competencyIds(),
                        sanitized.sources());
        EducationalContent content =
                new EducationalContent(
                        contentId,
                        sanitized.kind(),
                        slug,
                        actorId,
                        sanitized.subjectId(),
                        sanitized.educationLevelId(),
                        sanitized.parentId(),
                        ContentStatus.DRAFT,
                        null,
                        null,
                        null,
                        sanitized.sortOrder(),
                        sanitized.required(),
                        0,
                        now,
                        now);
        contents.save(content);
        revisions.save(revision);
        saveQuiz(revision.id(), sanitized);
        EducationalContent withRevision =
                new EducationalContent(
                        contentId,
                        sanitized.kind(),
                        slug,
                        actorId,
                        sanitized.subjectId(),
                        sanitized.educationLevelId(),
                        sanitized.parentId(),
                        ContentStatus.DRAFT,
                        revisionId,
                        null,
                        null,
                        sanitized.sortOrder(),
                        sanitized.required(),
                        0,
                        now,
                        now);
        saveContent(withRevision);
        contents.saveSlugHistory(slug, contentId);
        audit(actorId, AuditAction.CONTENT_CREATED, contentId, correlationId, Map.of("slug", slug));
        return requireContent(contentId);
    }

    @Transactional
    public EducationalContent update(
            UserId actorId, UUID contentId, ContentDraftInput input, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        EducationalContent content = requireOwned(actorId, contentId);
        ContentDraftInput sanitized = sanitize(input);
        AuthorizationPolicies.assertCanCreateContent(
                actor.user(),
                actor.permissions(),
                actor.approvedCompetencyIds(),
                Set.copyOf(sanitized.competencyIds()));
        Instant now = Instant.now();
        ContentRevision current = requireRevision(content.currentRevisionId());
        boolean currentWasSubmitted =
                submissions.listByContent(content.id()).stream()
                        .anyMatch(item -> item.revisionId().equals(current.id()));
        EducationalContent nextContent;
        if (content.status() == ContentStatus.DRAFT
                || (content.status() == ContentStatus.CHANGES_REQUESTED && !currentWasSubmitted)) {
            ContentLifecycle.assertCanEdit(content.status());
            revisions.updateMutable(
                    new ContentRevision(
                            current.id(),
                            content.id(),
                            current.revisionNumber(),
                            sanitized.title(),
                            sanitized.summary(),
                            sanitized.body(),
                            sanitized.license(),
                            sanitized.changeSummary(),
                            current.createdBy(),
                            current.createdAt(),
                            sanitized.competencyIds(),
                            sanitized.sources()));
            saveQuiz(current.id(), sanitized);
            nextContent =
                    withTaxonomy(
                            content, sanitized, content.currentRevisionId(), content.status(), now);
        } else if (content.status() == ContentStatus.CHANGES_REQUESTED
                || content.status() == ContentStatus.PUBLISHED) {
            int number = revisions.nextRevisionNumber(content.id());
            UUID revisionId = UUID.randomUUID();
            revisions.save(
                    new ContentRevision(
                            revisionId,
                            content.id(),
                            number,
                            sanitized.title(),
                            sanitized.summary(),
                            sanitized.body(),
                            sanitized.license(),
                            sanitized.changeSummary(),
                            actorId,
                            now,
                            sanitized.competencyIds(),
                            sanitized.sources()));
            saveQuiz(revisionId, sanitized);
            ContentStatus nextStatus =
                    content.status() == ContentStatus.PUBLISHED
                            ? ContentStatus.DRAFT
                            : ContentStatus.CHANGES_REQUESTED;
            if (content.status() == ContentStatus.PUBLISHED) {
                ContentLifecycle.assertTransition(ContentStatus.PUBLISHED, ContentStatus.DRAFT);
            }
            nextContent = withTaxonomy(content, sanitized, revisionId, nextStatus, now);
        } else {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_EDITABLE,
                    "Submitted or in-review revisions cannot be edited.");
        }
        saveContent(nextContent);
        audit(
                actorId,
                AuditAction.CONTENT_UPDATED,
                contentId,
                correlationId,
                Map.of("revisionId", nextContent.currentRevisionId().toString()));
        return requireContent(contentId);
    }

    @Transactional
    public EducationalContent submit(UserId actorId, UUID contentId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        EducationalContent content = requireOwned(actorId, contentId);
        ContentRevision revision = requireRevision(content.currentRevisionId());
        AuthorizationPolicies.assertCanCreateContent(
                actor.user(),
                actor.permissions(),
                actor.approvedCompetencyIds(),
                Set.copyOf(revision.competencyIds()));
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.CONTENT_SUBMIT);
        assertReadyToSubmit(content, revision);
        if (content.status() != ContentStatus.DRAFT
                && content.status() != ContentStatus.CHANGES_REQUESTED) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_CONTENT_TRANSITION, "Content cannot be submitted now.");
        }
        ContentLifecycle.assertTransition(content.status(), ContentStatus.SUBMITTED);
        Instant now = Instant.now();
        ContentSubmission submission =
                new ContentSubmission(
                        UUID.randomUUID(),
                        content.id(),
                        revision.id(),
                        content.makerId(),
                        SubmissionStatus.SUBMITTED,
                        null,
                        null,
                        null,
                        0,
                        now,
                        now);
        submissions.save(submission);
        saveContent(withStatus(content, ContentStatus.SUBMITTED, revision.id(), now));
        audit(
                actorId,
                AuditAction.CONTENT_SUBMITTED,
                contentId,
                correlationId,
                Map.of(
                        "revisionId",
                        revision.id().toString(),
                        "submissionId",
                        submission.id().toString(),
                        "revisionNumber",
                        revision.revisionNumber()));
        return requireContent(contentId);
    }

    @Transactional
    public EducationalContent publish(UserId actorId, UUID contentId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        EducationalContent content = requireContent(contentId);
        boolean owner = content.ownedBy(actorId);
        if (!owner) {
            AuthorizationPolicies.assertHasPermission(
                    actor.permissions(), Permission.CONTENT_PUBLISH);
            AuthorizationPolicies.assertActive(actor.user());
        } else {
            AuthorizationPolicies.assertActive(actor.user());
            AuthorizationPolicies.assertHasPermission(
                    actor.permissions(), Permission.CONTENT_PUBLISH);
        }
        if (content.status() != ContentStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_CONTENT_TRANSITION,
                    "Only approved content can be published.");
        }
        ContentRevision revision = requireRevision(content.currentRevisionId());
        Instant now = Instant.now();
        ContentLifecycle.assertTransition(ContentStatus.APPROVED, ContentStatus.PUBLISHED);
        EducationalContent published =
                content.withWorkflow(
                        ContentStatus.PUBLISHED, revision.id(), revision.id(), null, now);
        saveContent(published);
        indexPublished(published, revision);
        audit(
                actorId,
                AuditAction.CONTENT_PUBLISHED,
                contentId,
                correlationId,
                Map.of("revisionId", revision.id().toString()));
        return requireContent(contentId);
    }

    @Transactional
    public EducationalContent archive(UserId actorId, UUID contentId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        EducationalContent content = requireContent(contentId);
        AuthorizationPolicies.assertActive(actor.user());
        boolean owner = content.ownedBy(actorId);
        if (!owner) {
            AuthorizationPolicies.assertHasPermission(
                    actor.permissions(), Permission.CONTENT_ARCHIVE);
        } else {
            AuthorizationPolicies.assertHasPermission(
                    actor.permissions(), Permission.CONTENT_ARCHIVE);
        }
        if (content.archivedAt() != null) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_CONTENT_TRANSITION, "Content is already archived.");
        }
        Instant now = Instant.now();
        EducationalContent archived =
                content.withWorkflow(
                        ContentStatus.ARCHIVED,
                        content.currentRevisionId(),
                        content.publishedRevisionId(),
                        now,
                        now);
        saveContent(archived);
        searchIndex.delete(content.id().toString());
        audit(actorId, AuditAction.CONTENT_ARCHIVED, contentId, correlationId, Map.of());
        return requireContent(contentId);
    }

    private void saveContent(EducationalContent content) {
        if (!contents.update(content)) {
            throw new ConflictException(
                    ErrorCodes.CONCURRENT_MODIFICATION, "Content was modified concurrently.");
        }
    }

    private EducationalContent withTaxonomy(
            EducationalContent content,
            ContentDraftInput input,
            UUID revisionId,
            ContentStatus status,
            Instant now) {
        return new EducationalContent(
                content.id(),
                input.kind(),
                content.slug(),
                content.makerId(),
                input.subjectId(),
                input.educationLevelId(),
                input.parentId(),
                status,
                revisionId,
                content.publishedRevisionId(),
                content.archivedAt(),
                input.sortOrder(),
                input.required(),
                content.version(),
                content.createdAt(),
                now);
    }

    private EducationalContent withStatus(
            EducationalContent content, ContentStatus status, UUID revisionId, Instant now) {
        return content.withWorkflow(
                status, revisionId, content.publishedRevisionId(), content.archivedAt(), now);
    }

    private ContentDraftInput sanitize(ContentDraftInput input) {
        if (input == null) {
            throw new ValidationException("Content payload is required.");
        }
        ContentKind kind = input.kind() == null ? ContentKind.MATERIAL : input.kind();
        String title = ContentSanitizer.plainText(input.title());
        String summary = ContentSanitizer.plainText(input.summary());
        if (title.isBlank()) {
            throw new ValidationException("Title is required.");
        }
        if (input.subjectId() == null
                || subjects.findById(input.subjectId()).filter(Subject::active).isEmpty()) {
            throw new ValidationException("A governed subject is required.");
        }
        if (input.educationLevelId() == null
                || levels.findById(input.educationLevelId())
                        .filter(item -> item.active())
                        .isEmpty()) {
            throw new ValidationException("A governed education level is required.");
        }
        if (input.parentId() != null && contents.findById(input.parentId()).isEmpty()) {
            throw new ValidationException("Parent content was not found.");
        }
        LicenseCode license = input.license() == null ? LicenseCode.CC_BY_SA : input.license();
        List<UUID> competencyIds = uniqueCompetencies(input.competencyIds());
        for (UUID competencyId : competencyIds) {
            competencies
                    .findById(competencyId)
                    .filter(item -> item.active())
                    .orElseThrow(() -> new ValidationException("Competency is invalid."));
        }
        List<ContentSource> sources = sanitizeSources(input.sources());
        return new ContentDraftInput(
                kind,
                title,
                summary,
                input.subjectId(),
                input.educationLevelId(),
                input.parentId(),
                competencyIds,
                license,
                ContentSanitizer.sanitize(input.body()),
                sources,
                ContentSanitizer.plainText(input.changeSummary()),
                Math.max(0, input.sortOrder()),
                input.required(),
                input.quiz());
    }

    private void assertReadyToSubmit(EducationalContent content, ContentRevision revision) {
        if (revision.title() == null || revision.title().isBlank()) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_INCOMPLETE, "Title is required before submit.");
        }
        if (content.kind() == ContentKind.QUIZ) {
            QuizSpec spec =
                    quizSpecs
                            .findByRevision(revision.id())
                            .orElseThrow(
                                    () ->
                                            new BusinessRuleViolationException(
                                                    ErrorCodes.CONTENT_INCOMPLETE,
                                                    "Quiz questions are required before submit."));
            QuizDraftFactory.assertReady(spec);
        } else if (!ContentSanitizer.hasSubstance(revision.body())) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_INCOMPLETE, "Body content is required before submit.");
        }
        if (revision.competencyIds() == null || revision.competencyIds().isEmpty()) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_INCOMPLETE, "At least one competency is required.");
        }
        if (revision.license() == null) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_INCOMPLETE, "License is required before submit.");
        }
    }

    private void saveQuiz(UUID revisionId, ContentDraftInput input) {
        if (input.kind() != ContentKind.QUIZ) {
            return;
        }
        if (input.quiz() == null) {
            throw new ValidationException("Quiz definition is required.");
        }
        QuizSpec spec =
                QuizDraftFactory.fromDraft(
                        revisionId,
                        new QuizDraftFactory.QuizDraftInput(
                                input.quiz().passingScore(),
                                input.quiz().maxAttempts(),
                                input.quiz().required(),
                                input.quiz().questions() == null
                                        ? List.of()
                                        : input.quiz().questions().stream()
                                                .map(
                                                        question ->
                                                                new QuizDraftFactory.QuestionInput(
                                                                        question.id(),
                                                                        question.type(),
                                                                        question.prompt(),
                                                                        question.explanation(),
                                                                        question.difficulty(),
                                                                        question.competencyId(),
                                                                        question.reference(),
                                                                        question.options() == null
                                                                                ? List.of()
                                                                                : question
                                                                                        .options()
                                                                                        .stream()
                                                                                        .map(
                                                                                                option ->
                                                                                                        new QuizDraftFactory
                                                                                                                .OptionInput(
                                                                                                                option
                                                                                                                        .id(),
                                                                                                                option
                                                                                                                        .label(),
                                                                                                                option
                                                                                                                        .text(),
                                                                                                                option
                                                                                                                        .correct()))
                                                                                        .toList()))
                                                .toList()));
        quizSpecs.replace(spec);
    }

    private List<UUID> uniqueCompetencies(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ValidationException("At least one competency is required.");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private List<ContentSource> sanitizeSources(List<ContentSource> sources) {
        if (sources == null) {
            return List.of();
        }
        List<ContentSource> cleaned = new ArrayList<>();
        int order = 0;
        for (ContentSource source : sources) {
            if (source == null) {
                continue;
            }
            String title = ContentSanitizer.plainText(source.title());
            if (title.isBlank()) {
                continue;
            }
            cleaned.add(
                    new ContentSource(
                            source.id() == null ? UUID.randomUUID() : source.id(),
                            title,
                            ContentSanitizer.plainText(source.author()),
                            ContentSanitizer.plainText(source.publisher()),
                            ContentSanitizer.safeHttpUrl(source.url()),
                            ContentSanitizer.plainText(source.publicationInfo()),
                            ContentSanitizer.plainText(source.notes()),
                            order++));
        }
        return cleaned;
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int attempt = 0;
        while (contents.slugTaken(slug)) {
            attempt++;
            slug = base + "-" + Integer.toString(attempt, 36);
            if (attempt > 50) {
                slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return slug;
    }

    private EducationalContent requireOwned(UserId actorId, UUID contentId) {
        EducationalContent content = requireContent(contentId);
        if (!content.ownedBy(actorId)) {
            throw new NotFoundException("Content not found.");
        }
        return content;
    }

    private EducationalContent requireContent(UUID id) {
        return contents.findById(id).orElseThrow(() -> new NotFoundException("Content not found."));
    }

    private ContentRevision requireRevision(UUID id) {
        return revisions
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Revision not found."));
    }

    private void indexPublished(EducationalContent content, ContentRevision revision) {
        Subject subject = subjects.findById(content.subjectId()).orElseThrow();
        String body = ContentSanitizer.plainText(revision.body());
        if (content.kind() == ContentKind.QUIZ) {
            QuizSpec spec = quizSpecs.findByRevision(revision.id()).orElse(null);
            if (spec != null) {
                String prompts =
                        spec.questions().stream()
                                .map(id.belajarbersama.domain.learning.QuizQuestion::prompt)
                                .reduce((left, right) -> left + " " + right)
                                .orElse("");
                body = (body + " " + prompts).trim();
            }
        }
        searchIndex.index(
                new SearchDocument(
                        content.id().toString(),
                        content.kind().name(),
                        revision.title(),
                        body,
                        Map.of(
                                "summary",
                                revision.summary() == null ? "" : revision.summary(),
                                "subject",
                                subject.name(),
                                "slug",
                                content.slug())));
    }

    private void audit(
            UserId actor,
            AuditAction action,
            UUID target,
            String correlationId,
            Map<String, Object> metadata) {
        auditRecorder.record(
                AuditEvent.of(
                        actor, action, "EducationalContent", target, correlationId, metadata));
    }
}
