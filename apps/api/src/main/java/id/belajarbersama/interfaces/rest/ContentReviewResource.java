package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentQueryService;
import id.belajarbersama.application.content.ContentReviewService;
import id.belajarbersama.domain.content.ContentReview;
import id.belajarbersama.domain.content.ContentSubmission;
import id.belajarbersama.domain.content.EducationalContent;
import id.belajarbersama.domain.content.ReviewDecision;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.ContentDetailResponse;
import id.belajarbersama.interfaces.rest.dto.ReviewDecisionRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reviews")
public class ContentReviewResource {
    private final ContentReviewService reviews;
    private final ContentQueryService queries;
    private final RequestAuthContext auth;

    public ContentReviewResource(
            ContentReviewService reviews, ContentQueryService queries, RequestAuthContext auth) {
        this.reviews = reviews;
        this.queries = queries;
        this.auth = auth;
    }

    @GET
    @Path("/my")
    public List<ContentDetailResponse.SubmissionResponse> mine() {
        return reviews.queueFor(auth.requireUserId()).stream()
                .map(
                        item ->
                                ContentDetailResponse.submission(
                                        item, queries.titleForRevision(item.revisionId())))
                .toList();
    }

    @GET
    @Path("/{id}")
    public ContentDetailResponse get(@PathParam("id") UUID id) {
        ContentSubmission submission =
                queries.requireSubmissionForChecker(auth.requireUserId(), id);
        EducationalContent content = queries.requireKnown(submission.contentId());
        return ContentDetailResponse.of(
                content,
                queries.requireRevision(submission.revisionId()),
                queries.makerDisplayName(content.makerId()),
                queries.subject(content.subjectId()).name(),
                queries.level(content.educationLevelId()).name(),
                queries.reviewsForSubmission(submission.id()),
                queries.quizDraft(content));
    }

    @POST
    @Path("/{id}/start")
    public ContentDetailResponse.ContentReviewResponse start(@PathParam("id") UUID id) {
        ContentReview review = reviews.start(auth.requireUserId(), id, auth.correlationId());
        return ContentDetailResponse.review(review);
    }

    @POST
    @Path("/{id}/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContentDetailResponse.ContentReviewResponse approve(
            @PathParam("id") UUID id, ReviewDecisionRequest request) {
        return decide(id, ReviewDecision.APPROVE, request);
    }

    @POST
    @Path("/{id}/request-changes")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContentDetailResponse.ContentReviewResponse requestChanges(
            @PathParam("id") UUID id, ReviewDecisionRequest request) {
        return decide(id, ReviewDecision.REQUEST_CHANGES, request);
    }

    private ContentDetailResponse.ContentReviewResponse decide(
            UUID submissionId, ReviewDecision decision, ReviewDecisionRequest request) {
        ContentReview decided =
                reviews.decideForSubmission(
                        auth.requireUserId(),
                        submissionId,
                        decision,
                        request == null ? null : request.note(),
                        auth.correlationId());
        return ContentDetailResponse.review(decided);
    }
}
