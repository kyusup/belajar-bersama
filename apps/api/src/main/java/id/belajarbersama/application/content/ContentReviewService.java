package id.belajarbersama.application.content;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.content.ContentLifecycle;
import id.belajarbersama.domain.content.ContentReview;
import id.belajarbersama.domain.content.ContentReviewRepository;
import id.belajarbersama.domain.content.ContentRevision;
import id.belajarbersama.domain.content.ContentRevisionRepository;
import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.content.ContentStatus;
import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.ContentSubmissionRepository;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.content.ReviewDecision;
import id.belajarbersama.domain.content.SubmissionStatus;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ContentReviewService {
    private final CurrentUserQuery currentUserQuery;
    private final EducationalContentRepository contents;
    private final ContentRevisionRepository revisions;
    private final ContentSubmissionRepository submissions;
    private final ContentReviewRepository reviews;
    private final AuditRecorder auditRecorder;

    public ContentReviewService(
            CurrentUserQuery currentUserQuery,
            EducationalContentRepository contents,
            ContentRevisionRepository revisions,
            ContentSubmissionRepository submissions,
            ContentReviewRepository reviews,
            AuditRecorder auditRecorder) {
        this.currentUserQuery = currentUserQuery;
        this.contents = contents;
        this.revisions = revisions;
        this.submissions = submissions;
        this.reviews = reviews;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public ContentSubmission assign(
            UserId actorId, UUID contentId, UserId checkerId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.SYSTEM_ADMIN);
        EducationalContent content = requireContent(contentId);
        ContentSubmission open =
                submissions
                        .findOpenByContent(contentId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleViolationException(
                                                ErrorCodes.CONTENT_NOT_SUBMITTED,
                                                "There is no submitted content to assign."));
        var checker = currentUserQuery.load(checkerId);
        ContentRevision revision = requireRevision(open.revisionId());
        AuthorizationPolicies.assertCanReview(
                checker.user(),
                checker.storedRoles(),
                checker.approvedCompetencyIds(),
                Set.copyOf(revision.competencyIds()),
                content.makerId());
        Instant now = Instant.now();
        ContentSubmission updated =
                new ContentSubmission(
                        open.id(),
                        open.contentId(),
                        open.revisionId(),
                        open.makerId(),
                        open.status(),
                        checkerId,
                        actorId,
                        now,
                        open.version(),
                        open.createdAt(),
                        now);
        if (!submissions.updateIfVersion(updated, open.version())) {
            throw new ConflictException(
                    ErrorCodes.CONCURRENT_MODIFICATION, "Submission was modified concurrently.");
        }
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.CONTENT_REVIEW_ASSIGNED,
                        "ContentSubmission",
                        open.id(),
                        correlationId,
                        Map.of("checkerId", checkerId.value().toString())));
        return submissions.findById(open.id()).orElseThrow();
    }

    @Transactional
    public ContentReview start(UserId actorId, UUID submissionId, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        ContentSubmission submission = requireSubmission(submissionId);
        EducationalContent content = requireContent(submission.contentId());
        ContentRevision revision = requireRevision(submission.revisionId());
        AuthorizationPolicies.assertCanReview(
                actor.user(),
                actor.storedRoles(),
                actor.approvedCompetencyIds(),
                Set.copyOf(revision.competencyIds()),
                content.makerId());
        if (submission.status() != SubmissionStatus.SUBMITTED
                && submission.status() != SubmissionStatus.IN_REVIEW) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_REVIEWABLE, "Submission is not waiting for review.");
        }
        if (submission.assignedCheckerId() != null
                && !submission.assignedCheckerId().equals(actorId)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "This submission is assigned to another checker.");
        }
        if (submission.status() == SubmissionStatus.IN_REVIEW
                && actorId.equals(submission.assignedCheckerId())) {
            return reviews.listBySubmission(submission.id()).stream()
                    .filter(item -> item.decision() == null)
                    .filter(item -> item.reviewerId().equals(actorId))
                    .findFirst()
                    .orElseGet(() -> openReview(submission, actorId, Instant.now(), correlationId));
        }
        Instant now = Instant.now();
        ContentSubmission started =
                new ContentSubmission(
                        submission.id(),
                        submission.contentId(),
                        submission.revisionId(),
                        submission.makerId(),
                        SubmissionStatus.IN_REVIEW,
                        actorId,
                        submission.assignedBy() == null ? actorId : submission.assignedBy(),
                        submission.assignedAt() == null ? now : submission.assignedAt(),
                        submission.version(),
                        submission.createdAt(),
                        now);
        if (!submissions.updateIfVersion(started, submission.version())) {
            throw new ConflictException(
                    ErrorCodes.REVIEW_ALREADY_ACTIVE,
                    "Another checker already started this review.");
        }
        ContentLifecycle.assertTransition(content.status(), ContentStatus.IN_REVIEW);
        saveContentStatus(content, ContentStatus.IN_REVIEW, now);
        return openReview(started, actorId, now, correlationId);
    }

    private ContentReview openReview(
            ContentSubmission submission, UserId actorId, Instant now, String correlationId) {
        ContentReview review =
                new ContentReview(
                        UUID.randomUUID(),
                        submission.id(),
                        submission.revisionId(),
                        actorId,
                        null,
                        null,
                        now,
                        null);
        reviews.save(review);
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.CONTENT_REVIEW_STARTED,
                        "ContentSubmission",
                        submission.id(),
                        correlationId,
                        Map.of("revisionId", submission.revisionId().toString())));
        return review;
    }

    @Transactional
    public ContentReview decideForSubmission(
            UserId actorId,
            UUID submissionId,
            ReviewDecision decision,
            String comment,
            String correlationId) {
        ContentReview open =
                reviews.listBySubmission(submissionId).stream()
                        .filter(item -> item.decision() == null)
                        .filter(item -> item.reviewerId().equals(actorId))
                        .findFirst()
                        .orElse(null);
        if (open == null) {
            ContentSubmission submission = requireSubmission(submissionId);
            EducationalContent content = requireContent(submission.contentId());
            ContentRevision revision = requireRevision(submission.revisionId());
            var actor = currentUserQuery.load(actorId);
            AuthorizationPolicies.assertCanReview(
                    actor.user(),
                    actor.storedRoles(),
                    actor.approvedCompetencyIds(),
                    Set.copyOf(revision.competencyIds()),
                    content.makerId());
            throw new ValidationException("Start the review before recording a decision.");
        }
        return decide(actorId, open.id(), decision, comment, correlationId);
    }

    @Transactional
    public ContentReview decide(
            UserId actorId,
            UUID reviewId,
            ReviewDecision decision,
            String comment,
            String correlationId) {
        var actor = currentUserQuery.load(actorId);
        ContentReview review =
                reviews.findById(reviewId)
                        .orElseThrow(() -> new NotFoundException("Review not found."));
        if (review.decision() != null) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_REVIEWABLE, "This review already has a decision.");
        }
        ContentSubmission submission = requireSubmission(review.submissionId());
        EducationalContent content = requireContent(submission.contentId());
        ContentRevision revision = requireRevision(submission.revisionId());
        AuthorizationPolicies.assertCanReview(
                actor.user(),
                actor.storedRoles(),
                actor.approvedCompetencyIds(),
                Set.copyOf(revision.competencyIds()),
                content.makerId());
        if (!review.reviewerId().equals(actorId)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Only the assigned checker can record this decision.");
        }
        if (submission.status() != SubmissionStatus.IN_REVIEW) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_REVIEWABLE, "Submission is not in review.");
        }
        if (!submission.revisionId().equals(review.revisionId())) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_REVIEWABLE,
                    "Review is not tied to the current revision.");
        }
        if (decision == null) {
            throw new ValidationException("Review decision is required.");
        }
        String safeComment = ContentSanitizer.plainText(comment);
        Instant now = Instant.now();
        ContentReview decided =
                new ContentReview(
                        review.id(),
                        review.submissionId(),
                        review.revisionId(),
                        review.reviewerId(),
                        decision,
                        safeComment,
                        review.createdAt(),
                        now);
        reviews.update(decided);
        SubmissionStatus submissionStatus =
                decision == ReviewDecision.APPROVE
                        ? SubmissionStatus.APPROVED
                        : SubmissionStatus.CHANGES_REQUESTED;
        ContentStatus contentStatus =
                decision == ReviewDecision.APPROVE
                        ? ContentStatus.APPROVED
                        : ContentStatus.CHANGES_REQUESTED;
        ContentLifecycle.assertTransition(ContentStatus.IN_REVIEW, contentStatus);
        ContentSubmission closed =
                new ContentSubmission(
                        submission.id(),
                        submission.contentId(),
                        submission.revisionId(),
                        submission.makerId(),
                        submissionStatus,
                        submission.assignedCheckerId(),
                        submission.assignedBy(),
                        submission.assignedAt(),
                        submission.version(),
                        submission.createdAt(),
                        now);
        if (!submissions.updateIfVersion(closed, submission.version())) {
            throw new ConflictException(
                    ErrorCodes.CONCURRENT_MODIFICATION, "Submission was modified concurrently.");
        }
        saveContentStatus(content, contentStatus, now);
        AuditAction action =
                decision == ReviewDecision.APPROVE
                        ? AuditAction.CONTENT_APPROVED
                        : AuditAction.CONTENT_CHANGES_REQUESTED;
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        action,
                        "ContentReview",
                        review.id(),
                        correlationId,
                        Map.of(
                                "contentId",
                                content.id().toString(),
                                "revisionId",
                                review.revisionId().toString(),
                                "decision",
                                decision.name())));
        return decided;
    }

    public List<ContentSubmission> queueFor(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        if (!actor.storedRoles().contains(id.belajarbersama.domain.authorization.Role.CHECKER)
                && !actor.permissions().contains(Permission.SYSTEM_ADMIN)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Checker permission is required.");
        }
        List<ContentSubmission> queue = submissions.listQueue();
        List<ContentSubmission> assigned = submissions.listAssignedTo(actorId);
        Set<UUID> seen = new HashSet<>();
        List<ContentSubmission> result = new java.util.ArrayList<>();
        for (ContentSubmission item : assigned) {
            if (seen.add(item.id()) && eligible(actor, item)) {
                result.add(item);
            }
        }
        for (ContentSubmission item : queue) {
            if (seen.add(item.id()) && eligible(actor, item)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean eligible(CurrentUserQuery.CurrentUserView actor, ContentSubmission item) {
        EducationalContent content = contents.findById(item.contentId()).orElse(null);
        ContentRevision revision = revisions.findById(item.revisionId()).orElse(null);
        if (content == null || revision == null) {
            return false;
        }
        return AuthorizationPolicies.canReview(
                actor.user(),
                actor.storedRoles(),
                actor.approvedCompetencyIds(),
                Set.copyOf(revision.competencyIds()),
                content.makerId());
    }

    private void saveContentStatus(EducationalContent content, ContentStatus status, Instant now) {
        EducationalContent next =
                content.withWorkflow(
                        status,
                        content.currentRevisionId(),
                        content.publishedRevisionId(),
                        content.archivedAt(),
                        now);
        if (!contents.update(next)) {
            throw new ConflictException(
                    ErrorCodes.CONCURRENT_MODIFICATION, "Content was modified concurrently.");
        }
    }

    private EducationalContent requireContent(UUID id) {
        return contents.findById(id).orElseThrow(() -> new NotFoundException("Content not found."));
    }

    private ContentSubmission requireSubmission(UUID id) {
        return submissions
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Submission not found."));
    }

    private ContentRevision requireRevision(UUID id) {
        return revisions
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Revision not found."));
    }
}
