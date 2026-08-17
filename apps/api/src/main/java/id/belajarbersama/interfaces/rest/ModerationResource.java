package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentReportService;
import id.belajarbersama.application.qa.QaService;
import id.belajarbersama.domain.content.ContentReport;
import id.belajarbersama.domain.content.ReportStatus;
import id.belajarbersama.domain.qa.QaReport;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.ContentReportItemResponse;
import id.belajarbersama.interfaces.rest.dto.QaReportResponse;
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

@Path("/api/v1/moderation")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Moderation")
public class ModerationResource {
    private final QaService qa;
    private final ContentReportService contentReports;
    private final RequestAuthContext auth;
    private final PublicQaResource views;

    public ModerationResource(
            QaService qa,
            ContentReportService contentReports,
            RequestAuthContext auth,
            PublicQaResource views) {
        this.qa = qa;
        this.contentReports = contentReports;
        this.auth = auth;
        this.views = views;
    }

    @GET
    @Path("/reports")
    public Response qaReports() {
        List<QaReportResponse> items =
                qa.openReports(auth.requireUserId()).stream().map(this::qaReport).toList();
        return privateResponse(items);
    }

    @GET
    @Path("/content-reports")
    public Response contentReports() {
        List<ContentReportItemResponse> items =
                contentReports.listOpen(auth.requireUserId()).stream()
                        .map(this::contentReport)
                        .toList();
        return privateResponse(items);
    }

    @GET
    @Path("/qa/{id}")
    public Response question(@PathParam("id") UUID id) {
        return privateResponse(
                views.detail(
                        qa.requireForModeration(auth.requireUserId(), id),
                        auth.requireUserId(),
                        true));
    }

    @POST
    @Path("/qa/{id}/hide")
    public Response hideQuestion(@PathParam("id") UUID id) {
        qa.hideQuestion(auth.requireUserId(), id, auth.correlationId());
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @POST
    @Path("/qa/answers/{id}/hide")
    public Response hideAnswer(@PathParam("id") UUID id) {
        qa.hideAnswer(auth.requireUserId(), id, auth.correlationId());
        return Response.noContent().header("Cache-Control", "private, no-store").build();
    }

    @POST
    @Path("/reports/{id}/resolve")
    public Response resolveQa(@PathParam("id") UUID id) {
        return privateResponse(
                qaReport(
                        qa.resolveReport(
                                auth.requireUserId(),
                                id,
                                ReportStatus.RESOLVED,
                                auth.correlationId())));
    }

    @POST
    @Path("/reports/{id}/dismiss")
    public Response dismissQa(@PathParam("id") UUID id) {
        return privateResponse(
                qaReport(
                        qa.resolveReport(
                                auth.requireUserId(),
                                id,
                                ReportStatus.DISMISSED,
                                auth.correlationId())));
    }

    @POST
    @Path("/content-reports/{id}/resolve")
    public Response resolveContent(@PathParam("id") UUID id) {
        return privateResponse(
                contentReport(
                        contentReports.resolve(
                                auth.requireUserId(),
                                id,
                                ReportStatus.RESOLVED,
                                auth.correlationId())));
    }

    @POST
    @Path("/content-reports/{id}/dismiss")
    public Response dismissContent(@PathParam("id") UUID id) {
        return privateResponse(
                contentReport(
                        contentReports.resolve(
                                auth.requireUserId(),
                                id,
                                ReportStatus.DISMISSED,
                                auth.correlationId())));
    }

    private QaReportResponse qaReport(QaReport report) {
        return new QaReportResponse(
                report.id(),
                report.targetType().name(),
                report.targetId(),
                report.reason().name(),
                report.description(),
                report.status().name(),
                report.createdAt());
    }

    private ContentReportItemResponse contentReport(ContentReport report) {
        return new ContentReportItemResponse(
                report.id(),
                report.contentId(),
                report.reporterId().value(),
                report.reason().name(),
                report.description(),
                report.status().name(),
                report.createdAt());
    }

    private static Response privateResponse(Object entity) {
        return Response.ok(entity).header("Cache-Control", "private, no-store").build();
    }
}
