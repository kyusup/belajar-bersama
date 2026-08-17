package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.qa.QaService;
import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.qa.QaTargetType;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.QaAskRequest;
import id.belajarbersama.interfaces.rest.dto.QaBodyRequest;
import id.belajarbersama.interfaces.rest.dto.QaReportResponse;
import id.belajarbersama.interfaces.rest.dto.ReportRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/qa")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Qa")
public class QaResource {
    private final QaService qa;
    private final RequestAuthContext auth;
    private final PublicQaResource views;

    public QaResource(QaService qa, RequestAuthContext auth, PublicQaResource views) {
        this.qa = qa;
        this.auth = auth;
        this.views = views;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ask(QaAskRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required.");
        }
        return privateResponse(
                views.summary(
                        qa.ask(
                                auth.requireUserId(),
                                request.title(),
                                request.body(),
                                request.subjectId(),
                                request.contentId(),
                                auth.correlationId())));
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") UUID id, QaBodyRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required.");
        }
        return privateResponse(
                views.summary(
                        qa.updateQuestion(
                                auth.requireUserId(), id, request.title(), request.body())));
    }

    @POST
    @Path("/{id}/close")
    public Response close(@PathParam("id") UUID id) {
        return privateResponse(
                views.summary(qa.close(auth.requireUserId(), id, auth.correlationId())));
    }

    @POST
    @Path("/{id}/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response answer(@PathParam("id") UUID id, QaBodyRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required.");
        }
        qa.answer(auth.requireUserId(), id, request.body(), auth.correlationId());
        return privateResponse(views.detail(qa.requirePublic(id), auth.requireUserId(), false));
    }

    @PATCH
    @Path("/answers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAnswer(@PathParam("id") UUID id, QaBodyRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required.");
        }
        var answer = qa.updateAnswer(auth.requireUserId(), id, request.body());
        return privateResponse(
                views.detail(qa.requirePublic(answer.questionId()), auth.requireUserId(), false));
    }

    @POST
    @Path("/{id}/accept/{answerId}")
    public Response accept(@PathParam("id") UUID id, @PathParam("answerId") UUID answerId) {
        return privateResponse(
                views.detail(
                        qa.accept(auth.requireUserId(), id, answerId, auth.correlationId()),
                        auth.requireUserId(),
                        false));
    }

    @DELETE
    @Path("/{id}/accept")
    public Response unaccept(@PathParam("id") UUID id) {
        return privateResponse(
                views.detail(
                        qa.unaccept(auth.requireUserId(), id, auth.correlationId()),
                        auth.requireUserId(),
                        false));
    }

    @POST
    @Path("/answers/{id}/useful")
    public Response useful(@PathParam("id") UUID id) {
        qa.markUseful(auth.requireUserId(), id);
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @DELETE
    @Path("/answers/{id}/useful")
    public Response unuseful(@PathParam("id") UUID id) {
        qa.unmarkUseful(auth.requireUserId(), id);
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @POST
    @Path("/{id}/reports")
    @Consumes(MediaType.APPLICATION_JSON)
    public QaReportResponse reportQuestion(@PathParam("id") UUID id, ReportRequest request) {
        return report(QaTargetType.QUESTION, id, request);
    }

    @POST
    @Path("/answers/{id}/reports")
    @Consumes(MediaType.APPLICATION_JSON)
    public QaReportResponse reportAnswer(@PathParam("id") UUID id, ReportRequest request) {
        return report(QaTargetType.ANSWER, id, request);
    }

    private QaReportResponse report(QaTargetType type, UUID targetId, ReportRequest request) {
        if (request == null) {
            throw new ValidationException("Report body is required.");
        }
        ReportReason reason;
        try {
            reason = ReportReason.valueOf(request.reason().trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ValidationException("Unknown report reason.");
        }
        var saved =
                qa.report(
                        auth.requireUserId(),
                        type,
                        targetId,
                        reason,
                        request.description(),
                        auth.correlationId());
        return new QaReportResponse(
                saved.id(),
                saved.targetType().name(),
                saved.targetId(),
                saved.reason().name(),
                saved.description(),
                saved.status().name(),
                saved.createdAt());
    }

    private static Response privateResponse(Object entity) {
        return Response.ok(entity).header("Cache-Control", "private, no-store").build();
    }
}
