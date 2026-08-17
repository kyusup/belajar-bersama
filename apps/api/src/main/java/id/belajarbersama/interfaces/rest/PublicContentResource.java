package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentQueryService;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.taxonomy.EducationLevelRepository;
import id.belajarbersama.domain.taxonomy.Subject;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
import id.belajarbersama.interfaces.rest.dto.ContentDetailResponse;
import id.belajarbersama.interfaces.rest.dto.TaxonomyItemResponse;
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

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "PublicContent")
public class PublicContentResource {
    private final SubjectRepository subjects;
    private final EducationLevelRepository levels;
    private final ContentQueryService queries;

    public PublicContentResource(
            SubjectRepository subjects,
            EducationLevelRepository levels,
            ContentQueryService queries) {
        this.subjects = subjects;
        this.levels = levels;
        this.queries = queries;
    }

    @GET
    @Path("/subjects")
    public List<TaxonomyItemResponse> subjects() {
        return subjects.listActive().stream()
                .map(
                        item ->
                                new TaxonomyItemResponse(
                                        item.id(), item.slug(), item.name(), item.description()))
                .toList();
    }

    @GET
    @Path("/education-levels")
    public List<TaxonomyItemResponse> levels() {
        return levels.listActive().stream()
                .map(item -> new TaxonomyItemResponse(item.id(), item.slug(), item.name(), null))
                .toList();
    }

    @GET
    @Path("/public/subjects")
    public List<TaxonomyItemResponse> publicSubjects() {
        return subjects();
    }

    @GET
    @Path("/public/content")
    public Response publicList(
            @QueryParam("subject") String subjectSlug, @QueryParam("kind") String kind) {
        UUID subjectId = null;
        if (subjectSlug != null && !subjectSlug.isBlank()) {
            Subject subject =
                    subjects.findBySlug(subjectSlug)
                            .orElseThrow(
                                    () ->
                                            new id.belajarbersama.domain.error.NotFoundException(
                                                    "Subject not found."));
            subjectId = subject.id();
        }
        id.belajarbersama.domain.content.ContentKind parsed = null;
        if (kind != null && !kind.isBlank()) {
            try {
                parsed =
                        id.belajarbersama.domain.content.ContentKind.valueOf(
                                kind.trim().toUpperCase());
            } catch (Exception exception) {
                throw new id.belajarbersama.domain.error.ValidationException(
                        "Unknown content kind.");
            }
        }
        List<ContentDetailResponse> items =
                queries.publicList(parsed, subjectId).stream().map(this::publicDetail).toList();
        return cached(items);
    }

    @GET
    @Path("/public/content/{slug}")
    public Response publicContent(@PathParam("slug") String slug) {
        return cached(publicDetail(queries.requirePublic(slug)));
    }

    @GET
    @Path("/public/courses/{slug}")
    public Response publicCourse(@PathParam("slug") String slug) {
        return publicContent(slug);
    }

    @GET
    @Path("/public/learning-paths")
    public Response learningPaths(@QueryParam("subject") String subjectSlug) {
        return publicList(subjectSlug, "LEARNING_PATH");
    }

    @GET
    @Path("/public/courses")
    public Response courses(@QueryParam("subject") String subjectSlug) {
        return publicList(subjectSlug, "COURSE");
    }

    @GET
    @Path("/public/subjects/{slug}")
    public Response publicSubject(@PathParam("slug") String slug) {
        Subject subject =
                subjects.findBySlug(slug)
                        .orElseThrow(
                                () ->
                                        new id.belajarbersama.domain.error.NotFoundException(
                                                "Subject not found."));
        return cached(
                new TaxonomyItemResponse(
                        subject.id(), subject.slug(), subject.name(), subject.description()));
    }

    @GET
    @Path("/public/search")
    public Response search(@QueryParam("q") String q, @QueryParam("page") Integer page) {
        return cached(queries.searchPublic(q, page == null ? 0 : page, 20));
    }

    private ContentDetailResponse publicDetail(EducationalContent content) {
        return ContentDetailResponse.publicOf(
                content,
                queries.publishedRevision(content),
                queries.makerDisplayName(content.makerId()),
                queries.subject(content.subjectId()).name(),
                queries.level(content.educationLevelId()).name(),
                queries.publicChildren(content));
    }

    private static Response cached(Object entity) {
        return Response.ok(entity).header("Cache-Control", "public, max-age=30").build();
    }
}
