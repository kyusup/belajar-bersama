package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentQueryService;
import id.belajarbersama.application.learning.LearningService;
import id.belajarbersama.application.learning.QuizAttemptService;
import id.belajarbersama.domain.learning.AttemptAnswers;
import id.belajarbersama.domain.learning.AttemptStatus;
import id.belajarbersama.domain.learning.Bookmark;
import id.belajarbersama.domain.learning.LearningResume;
import id.belajarbersama.domain.learning.ProgressSnapshot;
import id.belajarbersama.domain.learning.QuizAttempt;
import id.belajarbersama.domain.learning.QuizScoring;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.AnswerRequest;
import id.belajarbersama.interfaces.rest.dto.AttemptResponse;
import id.belajarbersama.interfaces.rest.dto.BookmarkRequest;
import id.belajarbersama.interfaces.rest.dto.ContentDetailResponse;
import id.belajarbersama.interfaces.rest.dto.ProgressResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Learning")
public class LearningResource {
    private final LearningService learning;
    private final QuizAttemptService quizzes;
    private final ContentQueryService queries;
    private final RequestAuthContext auth;

    public LearningResource(
            LearningService learning,
            QuizAttemptService quizzes,
            ContentQueryService queries,
            RequestAuthContext auth) {
        this.learning = learning;
        this.quizzes = quizzes;
        this.queries = queries;
        this.auth = auth;
    }

    @GET
    @Path("/me/progress/{contentId}")
    public Response progress(@PathParam("contentId") UUID contentId) {
        ProgressSnapshot snapshot = learning.progress(auth.requireUserId(), contentId);
        return privateResponse(
                new ProgressResponse(
                        contentId,
                        snapshot.completed(),
                        snapshot.total(),
                        snapshot.percent(),
                        learning.lessonCompleted(auth.requireUserId(), contentId)));
    }

    @POST
    @Path("/me/lessons/{contentId}/complete")
    public Response complete(@PathParam("contentId") UUID contentId) {
        learning.completeLesson(auth.requireUserId(), contentId);
        return progress(contentId);
    }

    @POST
    @Path("/me/opened/{contentId}")
    public Response opened(@PathParam("contentId") UUID contentId) {
        learning.opened(auth.requireUserId(), contentId);
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @GET
    @Path("/me/continue")
    public Response resume() {
        LearningResume resume = learning.resume(auth.requireUserId());
        if (resume == null) {
            return privateResponse(null);
        }
        return queries.findPublic(resume.contentId())
                .map(
                        content ->
                                privateResponse(
                                        ContentDetailResponse.publicOf(
                                                content,
                                                queries.publishedRevision(content),
                                                queries.makerDisplayName(content.makerId()),
                                                queries.subject(content.subjectId()).name(),
                                                queries.level(content.educationLevelId()).name(),
                                                queries.publicChildren(content))))
                .orElseGet(() -> privateResponse(null));
    }

    @GET
    @Path("/me/bookmarks")
    public Response bookmarks() {
        List<Bookmark> items = learning.myBookmarks(auth.requireUserId());
        return privateResponse(
                items.stream()
                        .flatMap(item -> queries.findPublic(item.contentId()).stream())
                        .map(
                                content ->
                                        ContentDetailResponse.publicOf(
                                                content,
                                                queries.publishedRevision(content),
                                                queries.makerDisplayName(content.makerId()),
                                                queries.subject(content.subjectId()).name(),
                                                queries.level(content.educationLevelId()).name()))
                        .toList());
    }

    @POST
    @Path("/me/bookmarks")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addBookmark(BookmarkRequest request) {
        if (request == null || request.contentId() == null) {
            throw new id.belajarbersama.domain.error.ValidationException("contentId is required.");
        }
        learning.addBookmark(auth.requireUserId(), request.contentId());
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @DELETE
    @Path("/me/bookmarks/{contentId}")
    public Response deleteBookmark(@PathParam("contentId") UUID contentId) {
        learning.removeBookmark(auth.requireUserId(), contentId);
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @POST
    @Path("/me/quizzes/{quizId}/attempts")
    public Response start(@PathParam("quizId") UUID quizId) {
        return privateResponse(summary(quizzes.start(auth.requireUserId(), quizId), false));
    }

    @GET
    @Path("/me/quizzes/{quizId}/attempts")
    public Response history(@PathParam("quizId") UUID quizId) {
        return privateResponse(
                quizzes.history(auth.requireUserId(), quizId).stream()
                        .map(item -> summary(item, false))
                        .toList());
    }

    @GET
    @Path("/me/attempts/{id}")
    public Response attempt(@PathParam("id") UUID id) {
        QuizAttempt attempt = quizzes.requireOwnedAttempt(auth.requireUserId(), id);
        return privateResponse(summary(attempt, attempt.submitted()));
    }

    @POST
    @Path("/me/attempts/{id}/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response answers(@PathParam("id") UUID id, AnswerRequest request) {
        QuizAttempt attempt =
                quizzes.saveAnswers(
                        auth.requireUserId(),
                        id,
                        request == null ? java.util.Map.of() : request.asSets());
        return privateResponse(summary(attempt, false));
    }

    @POST
    @Path("/me/attempts/{id}/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submit(@PathParam("id") UUID id, AnswerRequest request) {
        QuizAttempt attempt =
                quizzes.submit(
                        auth.requireUserId(),
                        id,
                        request == null ? java.util.Map.of() : request.asSets());
        return privateResponse(summary(attempt, true));
    }

    @GET
    @Path("/me/quiz-history")
    public Response recent() {
        return privateResponse(
                quizzes.recent(auth.requireUserId()).stream()
                        .map(item -> summary(item, false))
                        .toList());
    }

    private AttemptResponse summary(QuizAttempt attempt, boolean includeReview) {
        AttemptAnswers stored = quizzes.answers(attempt.id());
        java.util.Map<UUID, List<UUID>> selected =
                stored.selectedByQuestion().entrySet().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        java.util.Map.Entry::getKey,
                                        entry -> List.copyOf(entry.getValue())));
        List<AttemptResponse.ReviewQuestion> review = List.of();
        if (includeReview && attempt.status() == AttemptStatus.SUBMITTED) {
            QuizSpec spec = quizzes.specForRevision(attempt.quizRevisionId());
            review =
                    spec.questions().stream()
                            .map(
                                    question ->
                                            new AttemptResponse.ReviewQuestion(
                                                    question.id(),
                                                    question.prompt(),
                                                    question.type().name(),
                                                    question.explanation(),
                                                    question.correctOptionIds(),
                                                    stored.selected(question.id()),
                                                    QuizScoring.questionCorrect(
                                                            question,
                                                            stored.selected(question.id())),
                                                    question.options().stream()
                                                            .map(PublicQuizResource::option)
                                                            .toList()))
                            .toList();
        }
        return new AttemptResponse(
                attempt.id(),
                attempt.quizId(),
                attempt.quizRevisionId(),
                attempt.status().name(),
                attempt.scorePercent(),
                attempt.passed(),
                attempt.correctCount(),
                attempt.questionCount(),
                attempt.startedAt(),
                attempt.submittedAt(),
                selected,
                review);
    }

    private static Response privateResponse(Object entity) {
        return Response.ok(entity).header("Cache-Control", "private, no-store").build();
    }
}
