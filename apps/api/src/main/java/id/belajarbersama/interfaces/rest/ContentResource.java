package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentCommandService;
import id.belajarbersama.application.content.ContentDraftInput;
import id.belajarbersama.application.content.ContentQueryService;
import id.belajarbersama.application.content.ContentReportService;
import id.belajarbersama.domain.content.ContentKind;
import id.belajarbersama.domain.content.ContentReport;
import id.belajarbersama.domain.content.ContentSource;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.LicenseCode;
import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.ContentDetailResponse;
import id.belajarbersama.interfaces.rest.dto.CreateContentRequest;
import id.belajarbersama.interfaces.rest.dto.LicenseResponse;
import id.belajarbersama.interfaces.rest.dto.ReportRequest;
import id.belajarbersama.interfaces.rest.dto.ReportResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Content")
public class ContentResource {
    private final ContentCommandService commands;
    private final ContentQueryService queries;
    private final ContentReportService reports;
    private final RequestAuthContext auth;

    public ContentResource(
            ContentCommandService commands,
            ContentQueryService queries,
            ContentReportService reports,
            RequestAuthContext auth) {
        this.commands = commands;
        this.queries = queries;
        this.reports = reports;
        this.auth = auth;
    }

    @GET
    @Path("/licenses")
    public List<LicenseResponse> licenses() {
        return List.of(
                new LicenseResponse(
                        "CC_BY_SA",
                        "CC BY-SA",
                        "Creative Commons Attribution-ShareAlike. Rekomendasi untuk karya asli."),
                new LicenseResponse(
                        "PUBLIC_DOMAIN", "Domain Publik", "Tidak ada hak cipta yang diketahui."),
                new LicenseResponse(
                        "ORIGINAL_WORK",
                        "Karya Asli",
                        "Memilih lisensi tidak memberikan hak yang tidak Anda miliki."),
                new LicenseResponse(
                        "EXTERNAL_ALL_RIGHTS_RESERVED",
                        "Eksternal / All Rights Reserved",
                        "Jangan klaim kepemilikan materi pihak ketiga."),
                new LicenseResponse(
                        "OTHER", "Lainnya", "Ketentuan lain yang dijelaskan pada sumber."));
    }

    @POST
    @Path("/content")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContentDetailResponse create(CreateContentRequest request) {
        EducationalContent content =
                commands.create(auth.requireUserId(), toInput(request), auth.correlationId());
        return detail(content, auth.requireUserId());
    }

    @GET
    @Path("/content/{id}")
    public ContentDetailResponse get(@PathParam("id") UUID id) {
        UserId actor = auth.userId().orElse(null);
        return detail(queries.requireForReader(actor, id), actor);
    }

    @PATCH
    @Path("/content/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContentDetailResponse patch(@PathParam("id") UUID id, CreateContentRequest request) {
        return detail(
                commands.update(auth.requireUserId(), id, toInput(request), auth.correlationId()),
                auth.requireUserId());
    }

    @POST
    @Path("/content/{id}/submit")
    public ContentDetailResponse submit(@PathParam("id") UUID id) {
        return detail(
                commands.submit(auth.requireUserId(), id, auth.correlationId()),
                auth.requireUserId());
    }

    @POST
    @Path("/content/{id}/publish")
    public ContentDetailResponse publish(@PathParam("id") UUID id) {
        return detail(
                commands.publish(auth.requireUserId(), id, auth.correlationId()),
                auth.requireUserId());
    }

    @POST
    @Path("/content/{id}/archive")
    public ContentDetailResponse archive(@PathParam("id") UUID id) {
        return detail(
                commands.archive(auth.requireUserId(), id, auth.correlationId()),
                auth.requireUserId());
    }

    @GET
    @Path("/my/content")
    public List<ContentDetailResponse> mine() {
        UserId actor = auth.requireUserId();
        return queries.myContent(actor).stream().map(item -> detail(item, actor)).toList();
    }

    @GET
    @Path("/my/content/{id}/revisions")
    public List<ContentDetailResponse.ContentRevisionResponse> myRevisions(
            @PathParam("id") UUID id) {
        return queries.revisionsForOwner(auth.requireUserId(), id).stream()
                .map(ContentDetailResponse::revision)
                .toList();
    }

    @POST
    @Path("/content/{id}/reports")
    @Consumes(MediaType.APPLICATION_JSON)
    public ReportResponse report(@PathParam("id") UUID id, ReportRequest request) {
        if (request == null) {
            throw new ValidationException("Report body is required.");
        }
        ReportReason reason;
        try {
            reason = ReportReason.valueOf(request.reason().trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ValidationException("Unknown report reason.");
        }
        ContentReport saved =
                reports.report(
                        auth.requireUserId(),
                        id,
                        reason,
                        request.description(),
                        auth.correlationId());
        return new ReportResponse(saved.id(), saved.contentId(), saved.status().name());
    }

    private ContentDetailResponse detail(EducationalContent content, UserId actor) {
        boolean privileged = queries.canSeeWorkflow(actor, content);
        if (!privileged) {
            return ContentDetailResponse.publicOf(
                    content,
                    queries.publishedRevision(content),
                    queries.makerDisplayName(content.makerId()),
                    queries.subject(content.subjectId()).name(),
                    queries.level(content.educationLevelId()).name());
        }
        return ContentDetailResponse.of(
                content,
                queries.currentRevision(content),
                queries.makerDisplayName(content.makerId()),
                queries.subject(content.subjectId()).name(),
                queries.level(content.educationLevelId()).name(),
                queries.reviewsForMaker(actor, content.id()),
                queries.quizDraft(content));
    }

    static ContentDraftInput toInput(CreateContentRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required.");
        }
        List<ContentSource> sources =
                request.sources() == null
                        ? List.of()
                        : request.sources().stream()
                                .map(
                                        item ->
                                                new ContentSource(
                                                        UUID.randomUUID(),
                                                        item.title(),
                                                        item.author(),
                                                        item.publisher(),
                                                        item.url(),
                                                        item.publicationInfo(),
                                                        item.notes(),
                                                        0))
                                .toList();
        try {
            return new ContentDraftInput(
                    request.kind() == null
                            ? ContentKind.MATERIAL
                            : ContentKind.valueOf(request.kind().trim().toUpperCase(Locale.ROOT)),
                    request.title(),
                    request.summary(),
                    request.subjectId(),
                    request.educationLevelId(),
                    request.parentId(),
                    request.competencyIds(),
                    request.license() == null
                            ? LicenseCode.CC_BY_SA
                            : LicenseCode.valueOf(
                                    request.license().trim().toUpperCase(Locale.ROOT)),
                    request.body(),
                    sources,
                    request.changeSummary(),
                    request.sortOrder() == null ? 0 : request.sortOrder(),
                    request.required() == null || request.required(),
                    request.quiz() == null
                            ? null
                            : new ContentDraftInput.QuizDraft(
                                    request.quiz().passingScore(),
                                    request.quiz().maxAttempts(),
                                    request.quiz().required(),
                                    request.quiz().questions() == null
                                            ? List.of()
                                            : request.quiz().questions().stream()
                                                    .map(
                                                            question ->
                                                                    new ContentDraftInput
                                                                            .QuestionDraft(
                                                                            question.id(),
                                                                            question.type(),
                                                                            question.prompt(),
                                                                            question.explanation(),
                                                                            question.difficulty(),
                                                                            question.competencyId(),
                                                                            question.reference(),
                                                                            question.options()
                                                                                            == null
                                                                                    ? List.of()
                                                                                    : question
                                                                                            .options()
                                                                                            .stream()
                                                                                            .map(
                                                                                                    option ->
                                                                                                            new ContentDraftInput
                                                                                                                    .OptionDraft(
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
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Invalid content kind or license.");
        }
    }
}
