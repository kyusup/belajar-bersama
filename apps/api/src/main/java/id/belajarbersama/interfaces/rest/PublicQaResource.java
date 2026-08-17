package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.qa.QaService;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.qa.QaAnswer;
import id.belajarbersama.domain.qa.QaQuestion;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.QaAnswerResponse;
import id.belajarbersama.interfaces.rest.dto.QaPageResponse;
import id.belajarbersama.interfaces.rest.dto.QaQuestionResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/public/qa")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "PublicQa")
public class PublicQaResource {
    private final QaService qa;
    private final RequestAuthContext auth;

    public PublicQaResource(QaService qa, RequestAuthContext auth) {
        this.qa = qa;
        this.auth = auth;
    }

    @GET
    public Response list(
            @QueryParam("contentId") UUID contentId,
            @QueryParam("content") UUID content,
            @QueryParam("subjectId") UUID subjectId,
            @QueryParam("subject") UUID subject,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        UUID resolvedContent = contentId != null ? contentId : content;
        UUID resolvedSubject = subjectId != null ? subjectId : subject;
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        List<QaQuestionResponse> items =
                qa.listPublic(resolvedContent, resolvedSubject, pageNumber, pageSize).stream()
                        .map(this::summary)
                        .toList();
        return Response.ok(
                        new QaPageResponse(
                                items,
                                pageNumber,
                                pageSize,
                                qa.countPublic(resolvedContent, resolvedSubject)))
                .header("Cache-Control", "public, max-age=30")
                .build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        UserId viewer = auth.userId().orElse(null);
        return Response.ok(detail(qa.requirePublic(id), viewer, false))
                .header("Cache-Control", "public, max-age=15")
                .build();
    }

    QaQuestionResponse summary(QaQuestion question) {
        return new QaQuestionResponse(
                question.id(),
                question.title(),
                question.body(),
                question.authorId().value(),
                qa.displayName(question.authorId()),
                question.subjectId(),
                question.contentId(),
                question.status().name(),
                question.acceptedAnswerId(),
                question.createdAt(),
                question.updatedAt(),
                List.of());
    }

    QaQuestionResponse detail(QaQuestion question, UserId viewer, boolean includeHidden) {
        List<QaAnswerResponse> answers =
                qa.answers(question.id(), includeHidden).stream()
                        .map(answer -> toAnswer(question, answer, viewer))
                        .toList();
        return new QaQuestionResponse(
                question.id(),
                question.title(),
                question.body(),
                question.authorId().value(),
                qa.displayName(question.authorId()),
                question.subjectId(),
                question.contentId(),
                question.status().name(),
                question.acceptedAnswerId(),
                question.createdAt(),
                question.updatedAt(),
                answers);
    }

    private QaAnswerResponse toAnswer(QaQuestion question, QaAnswer answer, UserId viewer) {
        return new QaAnswerResponse(
                answer.id(),
                answer.questionId(),
                answer.authorId().value(),
                qa.displayName(answer.authorId()),
                answer.body(),
                answer.id().equals(question.acceptedAnswerId()),
                qa.usefulCount(answer.id()),
                qa.markedUseful(viewer, answer.id()),
                answer.createdAt());
    }
}
