package id.belajarbersama.application.content;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.ContentReport;
import id.belajarbersama.domain.content.ContentReportRepository;
import id.belajarbersama.domain.content.ContentReview;
import id.belajarbersama.domain.content.ContentReviewRepository;
import id.belajarbersama.domain.content.ContentRevision;
import id.belajarbersama.domain.content.ContentRevisionRepository;
import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.ContentSubmissionRepository;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.learning.QuizOption;
import id.belajarbersama.domain.learning.QuizQuestion;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.domain.learning.QuizSpecRepository;
import id.belajarbersama.domain.search.SearchIndex;
import id.belajarbersama.domain.search.SearchPage;
import id.belajarbersama.domain.search.SearchQuery;
import id.belajarbersama.domain.taxonomy.EducationLevel;
import id.belajarbersama.domain.taxonomy.EducationLevelRepository;
import id.belajarbersama.domain.taxonomy.Subject;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
import id.belajarbersama.interfaces.rest.dto.ContentDetailResponse;
import id.belajarbersama.interfaces.rest.dto.CreateContentRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ContentQueryService {
    private final EducationalContentRepository contents;
    private final ContentRevisionRepository revisions;
    private final ContentSubmissionRepository submissions;
    private final ContentReviewRepository reviews;
    private final ContentReportRepository reports;
    private final SubjectRepository subjects;
    private final EducationLevelRepository levels;
    private final UserRepository users;
    private final SearchIndex searchIndex;
    private final CurrentUserQuery currentUserQuery;
    private final QuizSpecRepository quizSpecs;

    public ContentQueryService(
            EducationalContentRepository contents,
            ContentRevisionRepository revisions,
            ContentSubmissionRepository submissions,
            ContentReviewRepository reviews,
            ContentReportRepository reports,
            SubjectRepository subjects,
            EducationLevelRepository levels,
            UserRepository users,
            SearchIndex searchIndex,
            CurrentUserQuery currentUserQuery,
            QuizSpecRepository quizSpecs) {
        this.contents = contents;
        this.revisions = revisions;
        this.submissions = submissions;
        this.reviews = reviews;
        this.reports = reports;
        this.subjects = subjects;
        this.levels = levels;
        this.users = users;
        this.searchIndex = searchIndex;
        this.currentUserQuery = currentUserQuery;
        this.quizSpecs = quizSpecs;
    }

    public EducationalContent requirePublicById(UUID id) {
        EducationalContent content =
                contents.findById(id)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.publiclyVisible()) {
            throw new NotFoundException("Content not found.");
        }
        return content;
    }

    public java.util.Optional<EducationalContent> findPublic(UUID id) {
        return contents.findById(id).filter(EducationalContent::publiclyVisible);
    }

    public EducationalContent requirePublic(String slug) {
        EducationalContent content =
                contents.findBySlug(slug)
                        .or(
                                () ->
                                        contents.contentIdForSlugHistory(slug)
                                                .flatMap(contents::findById))
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.publiclyVisible()) {
            throw new NotFoundException("Content not found.");
        }
        return content;
    }

    public EducationalContent requireKnown(UUID id) {
        return contents.findById(id).orElseThrow(() -> new NotFoundException("Content not found."));
    }

    public EducationalContent requireForReader(UserId actorId, UUID id) {
        EducationalContent content =
                contents.findById(id)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (content.publiclyVisible()) {
            return content;
        }
        if (actorId != null && content.ownedBy(actorId)) {
            return content;
        }
        if (actorId != null && isAssignedChecker(actorId, content.id())) {
            return content;
        }
        throw new NotFoundException("Content not found.");
    }

    public String titleForRevision(UUID revisionId) {
        return revisions.findById(revisionId).map(ContentRevision::title).orElse("");
    }

    public ContentRevision requireRevision(UUID id) {
        return revisions
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Revision not found."));
    }

    public ContentRevision publishedRevision(EducationalContent content) {
        if (content.publishedRevisionId() == null) {
            throw new NotFoundException("Content not found.");
        }
        return revisions
                .findById(content.publishedRevisionId())
                .orElseThrow(() -> new NotFoundException("Content not found."));
    }

    public ContentRevision currentRevision(EducationalContent content) {
        return revisions
                .findById(content.currentRevisionId())
                .orElseThrow(() -> new NotFoundException("Revision not found."));
    }

    public List<ContentRevision> revisionsForOwner(UserId actorId, UUID contentId) {
        EducationalContent content =
                contents.findById(contentId)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.ownedBy(actorId)) {
            throw new AuthorizationException(ErrorCodes.FORBIDDEN, "Revision history is private.");
        }
        return revisions.listByContent(contentId);
    }

    public List<EducationalContent> myContent(UserId actorId) {
        return contents.listByMaker(actorId);
    }

    public List<EducationalContent> publicBySubject(UUID subjectId) {
        return contents.listPublicBySubject(subjectId);
    }

    public List<EducationalContent> publicAll() {
        return contents.listPublic();
    }

    public List<EducationalContent> publicList(ContentKind kind, UUID subjectId) {
        return contents.listPublic(kind, subjectId);
    }

    public List<ContentDetailResponse.ChildResponse> publicChildren(EducationalContent parent) {
        return outline(parent.id());
    }

    private List<ContentDetailResponse.ChildResponse> outline(UUID parentId) {
        return contents.listPublicChildren(parentId).stream()
                .map(
                        child ->
                                new ContentDetailResponse.ChildResponse(
                                        child.id(),
                                        child.slug(),
                                        child.kind().name(),
                                        publishedRevision(child).title(),
                                        child.sortOrder(),
                                        child.required(),
                                        outline(child.id())))
                .toList();
    }

    public Subject subject(UUID id) {
        return subjects.findById(id).orElseThrow(() -> new NotFoundException("Subject not found."));
    }

    public EducationLevel level(UUID id) {
        return levels.findById(id)
                .orElseThrow(() -> new NotFoundException("Education level not found."));
    }

    public String makerDisplayName(UserId makerId) {
        return users.findById(makerId).map(User::displayName).orElse("Kontributor");
    }

    public boolean canSeeWorkflow(UserId actorId, EducationalContent content) {
        return actorId != null
                && (content.ownedBy(actorId) || isAssignedChecker(actorId, content.id()));
    }

    public List<ContentReview> reviewsForMaker(UserId actorId, UUID contentId) {
        EducationalContent content =
                contents.findById(contentId)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.ownedBy(actorId) && !isAssignedChecker(actorId, contentId)) {
            throw new AuthorizationException(ErrorCodes.FORBIDDEN, "Reviews are not visible.");
        }
        return reviews.listByContent(contentId);
    }

    public ContentSubmission requireSubmissionForChecker(UserId actorId, UUID submissionId) {
        ContentSubmission submission =
                submissions
                        .findById(submissionId)
                        .orElseThrow(() -> new NotFoundException("Submission not found."));
        var actor = currentUserQuery.load(actorId);
        EducationalContent content = requireForReader(actorId, submission.contentId());
        ContentRevision revision =
                revisions
                        .findById(submission.revisionId())
                        .orElseThrow(() -> new NotFoundException("Revision not found."));
        if (content.ownedBy(actorId)) {
            return submission;
        }
        if (!id.belajarbersama.domain.authorization.AuthorizationPolicies.canReview(
                actor.user(),
                actor.storedRoles(),
                actor.approvedCompetencyIds(),
                java.util.Set.copyOf(revision.competencyIds()),
                content.makerId())) {
            throw new NotFoundException("Submission not found.");
        }
        return submission;
    }

    public List<ContentReview> reviewsForSubmission(UUID submissionId) {
        return reviews.listBySubmission(submissionId);
    }

    public ContentReview requireReview(UUID reviewId) {
        return reviews.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found."));
    }

    public SearchPage searchPublic(String text, int page, int size) {
        return searchIndex.search(new SearchQuery(text == null ? "" : text, page, size));
    }

    public List<ContentReport> reportsForContent(UUID contentId) {
        return reports.listByContent(contentId);
    }

    public CreateContentRequest.QuizRequest quizDraft(EducationalContent content) {
        if (content.kind() != ContentKind.QUIZ || content.currentRevisionId() == null) {
            return null;
        }
        return quizSpecs
                .findByRevision(content.currentRevisionId())
                .map(this::toQuizRequest)
                .orElse(null);
    }

    private CreateContentRequest.QuizRequest toQuizRequest(QuizSpec spec) {
        return new CreateContentRequest.QuizRequest(
                spec.passingScore(),
                spec.maxAttempts(),
                spec.required(),
                spec.questions().stream().map(this::toQuestion).toList());
    }

    private CreateContentRequest.QuestionRequest toQuestion(QuizQuestion question) {
        return new CreateContentRequest.QuestionRequest(
                question.id(),
                question.type().name(),
                question.prompt(),
                question.explanation(),
                question.difficulty().name(),
                question.competencyId(),
                question.reference(),
                question.options().stream().map(this::toOption).toList());
    }

    private CreateContentRequest.OptionRequest toOption(QuizOption option) {
        return new CreateContentRequest.OptionRequest(
                option.id(), option.label(), option.text(), option.correct());
    }

    private boolean isAssignedChecker(UserId actorId, UUID contentId) {
        return submissions
                .findOpenByContent(contentId)
                .filter(item -> actorId.equals(item.assignedCheckerId()))
                .isPresent();
    }
}
