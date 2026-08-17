package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentQueryService;
import id.belajarbersama.application.learning.QuizAttemptService;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.learning.QuizOption;
import id.belajarbersama.domain.learning.QuizQuestion;
import id.belajarbersama.domain.learning.QuizSpec;
import id.belajarbersama.interfaces.rest.dto.PublicQuizResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/public/quizzes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "PublicQuiz")
public class PublicQuizResource {
    private final ContentQueryService queries;
    private final QuizAttemptService quizzes;

    public PublicQuizResource(ContentQueryService queries, QuizAttemptService quizzes) {
        this.queries = queries;
        this.quizzes = quizzes;
    }

    @GET
    @Path("/{slug}")
    public Response get(@PathParam("slug") String slug) {
        EducationalContent content = queries.requirePublic(slug);
        EducationalContent quiz = quizzes.requirePublishedQuiz(content.id());
        QuizSpec spec = quizzes.specForRevision(quiz.publishedRevisionId());
        var revision = queries.publishedRevision(quiz);
        PublicQuizResponse body =
                new PublicQuizResponse(
                        quiz.id(),
                        quiz.slug(),
                        revision.title(),
                        revision.summary(),
                        spec.passingScore(),
                        spec.maxAttempts(),
                        spec.required(),
                        revision.id(),
                        revision.revisionNumber(),
                        spec.questions().stream().map(PublicQuizResource::question).toList());
        return Response.ok(body).header("Cache-Control", "public, max-age=30").build();
    }

    static PublicQuizResponse.Question question(QuizQuestion question) {
        return new PublicQuizResponse.Question(
                question.id(),
                question.type().name(),
                question.prompt(),
                question.difficulty().name(),
                question.reference(),
                question.options().stream().map(PublicQuizResource::option).toList());
    }

    static PublicQuizResponse.Option option(QuizOption option) {
        return new PublicQuizResponse.Option(option.id(), option.label(), option.text());
    }
}
