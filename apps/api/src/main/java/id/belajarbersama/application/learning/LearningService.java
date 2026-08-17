package id.belajarbersama.application.learning;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.learning.Bookmark;
import id.belajarbersama.domain.learning.BookmarkRepository;
import id.belajarbersama.domain.learning.LearningActivityKind;
import id.belajarbersama.domain.learning.LearningActivityRepository;
import id.belajarbersama.domain.learning.LearningResume;
import id.belajarbersama.domain.learning.LessonCompletion;
import id.belajarbersama.domain.learning.LessonCompletionRepository;
import id.belajarbersama.domain.learning.ProgressSnapshot;
import id.belajarbersama.domain.learning.QuizAttemptRepository;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.domain.learning.QuizSpecRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class LearningService {
    private final CurrentUserQuery currentUserQuery;
    private final EducationalContentRepository contents;
    private final LessonCompletionRepository completions;
    private final BookmarkRepository bookmarks;
    private final LearningActivityRepository activities;
    private final QuizAttemptRepository attempts;
    private final QuizSpecRepository quizSpecs;

    public LearningService(
            CurrentUserQuery currentUserQuery,
            EducationalContentRepository contents,
            LessonCompletionRepository completions,
            BookmarkRepository bookmarks,
            LearningActivityRepository activities,
            QuizAttemptRepository attempts,
            QuizSpecRepository quizSpecs) {
        this.currentUserQuery = currentUserQuery;
        this.contents = contents;
        this.completions = completions;
        this.bookmarks = bookmarks;
        this.activities = activities;
        this.attempts = attempts;
        this.quizSpecs = quizSpecs;
    }

    @Transactional
    public void completeLesson(UserId actorId, UUID contentId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.LEARNING_PROGRESS_MANAGE);
        EducationalContent content = requirePublished(contentId, ErrorCodes.LESSON_NOT_PUBLISHED);
        if (content.kind() != ContentKind.LESSON && content.kind() != ContentKind.MATERIAL) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_AVAILABLE,
                    "Only lessons and materials can be marked complete.");
        }
        Instant now = Instant.now();
        boolean created =
                completions.complete(
                        new LessonCompletion(
                                actorId, content.id(), content.publishedRevisionId(), now));
        if (created) {
            activities.save(
                    UUID.randomUUID(),
                    actorId,
                    content.id(),
                    LearningActivityKind.LESSON_COMPLETED,
                    now);
            maybeStartCourse(actorId, content, now);
        }
        touchResume(actorId, content, now);
    }

    @Transactional
    public void opened(UserId actorId, UUID contentId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        EducationalContent content = requirePublished(contentId, ErrorCodes.CONTENT_NOT_AVAILABLE);
        touchResume(actorId, content, Instant.now());
    }

    @Transactional
    public Bookmark addBookmark(UserId actorId, UUID contentId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.BOOKMARK_MANAGE);
        EducationalContent content = requirePublished(contentId, ErrorCodes.CONTENT_NOT_AVAILABLE);
        Instant now = Instant.now();
        Bookmark bookmark = new Bookmark(actorId, content.id(), now);
        if (!bookmarks.exists(actorId, content.id())) {
            bookmarks.save(bookmark);
            activities.save(
                    UUID.randomUUID(),
                    actorId,
                    content.id(),
                    LearningActivityKind.BOOKMARK_CREATED,
                    now);
        }
        return bookmark;
    }

    @Transactional
    public void removeBookmark(UserId actorId, UUID contentId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.BOOKMARK_MANAGE);
        bookmarks.delete(actorId, contentId);
    }

    public List<Bookmark> myBookmarks(UserId actorId) {
        requireSelf(actorId);
        return bookmarks.listByUser(actorId);
    }

    public boolean bookmarked(UserId actorId, UUID contentId) {
        return bookmarks.exists(actorId, contentId);
    }

    public boolean lessonCompleted(UserId actorId, UUID contentId) {
        return completions.exists(actorId, contentId);
    }

    public ProgressSnapshot progress(UserId actorId, UUID contentId) {
        requireSelf(actorId);
        EducationalContent root = requirePublished(contentId, ErrorCodes.CONTENT_NOT_AVAILABLE);
        List<EducationalContent> items = requiredItems(root);
        int completed = 0;
        List<UUID> lessonIds = new ArrayList<>();
        for (EducationalContent item : items) {
            if (item.kind() == ContentKind.QUIZ) {
                if (quizComplete(actorId, item)) {
                    completed++;
                }
            } else {
                lessonIds.add(item.id());
            }
        }
        Set<UUID> done = completions.completedContentIds(actorId, lessonIds);
        completed += done.size();
        return ProgressSnapshot.of(completed, items.size());
    }

    public LearningResume resume(UserId actorId) {
        requireSelf(actorId);
        return activities.resume(actorId).orElse(null);
    }

    public List<EducationalContent> requiredItems(EducationalContent root) {
        List<EducationalContent> tree = contents.listPublishedDescendants(root.id());
        List<EducationalContent> items = new ArrayList<>();
        for (EducationalContent item : tree) {
            if (item.id().equals(root.id())
                    && (root.kind() == ContentKind.COURSE
                            || root.kind() == ContentKind.MODULE
                            || root.kind() == ContentKind.LEARNING_PATH)) {
                continue;
            }
            if (!item.required()) {
                continue;
            }
            if (item.kind() == ContentKind.LESSON || item.kind() == ContentKind.MATERIAL) {
                items.add(item);
            } else if (item.kind() == ContentKind.QUIZ) {
                QuizSpec spec = quizSpecs.findByRevision(item.publishedRevisionId()).orElse(null);
                if (spec == null || spec.required()) {
                    items.add(item);
                }
            }
        }
        if (items.isEmpty()
                && (root.kind() == ContentKind.LESSON || root.kind() == ContentKind.MATERIAL)
                && root.required()) {
            items.add(root);
        }
        if (items.isEmpty() && root.kind() == ContentKind.QUIZ) {
            items.add(root);
        }
        return items;
    }

    private boolean quizComplete(UserId actorId, EducationalContent quiz) {
        QuizSpec spec = quizSpecs.findByRevision(quiz.publishedRevisionId()).orElse(null);
        if (spec != null && spec.passingScore() != null) {
            return attempts.hasPassingSubmission(actorId, quiz.id());
        }
        return attempts.hasSubmittedAttempt(actorId, quiz.id());
    }

    private void maybeStartCourse(UserId actorId, EducationalContent content, Instant now) {
        EducationalContent cursor = content;
        while (cursor.parentId() != null) {
            EducationalContent parent = contents.findById(cursor.parentId()).orElse(null);
            if (parent == null) {
                return;
            }
            if (parent.kind() == ContentKind.COURSE && parent.publiclyVisible()) {
                activities.save(
                        UUID.randomUUID(),
                        actorId,
                        parent.id(),
                        LearningActivityKind.COURSE_STARTED,
                        now);
                return;
            }
            cursor = parent;
        }
    }

    private void touchResume(UserId actorId, EducationalContent content, Instant now) {
        UUID courseId = nearestCourse(content);
        activities.upsertResume(new LearningResume(actorId, content.id(), courseId, now));
    }

    private UUID nearestCourse(EducationalContent content) {
        if (content.kind() == ContentKind.COURSE) {
            return content.id();
        }
        EducationalContent cursor = content;
        while (cursor.parentId() != null) {
            EducationalContent parent = contents.findById(cursor.parentId()).orElse(null);
            if (parent == null) {
                return null;
            }
            if (parent.kind() == ContentKind.COURSE) {
                return parent.id();
            }
            cursor = parent;
        }
        return null;
    }

    private EducationalContent requirePublished(UUID contentId, String code) {
        EducationalContent content =
                contents.findById(contentId)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.publiclyVisible()) {
            throw new BusinessRuleViolationException(code, "Published content is required.");
        }
        return content;
    }

    private void requireSelf(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        if (!actor.user().id().equals(actorId)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Learners may only access their own data.");
        }
    }
}
