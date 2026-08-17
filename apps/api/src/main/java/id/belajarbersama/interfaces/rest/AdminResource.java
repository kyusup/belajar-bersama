package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.content.ContentReviewService;
import id.belajarbersama.application.content.TaxonomyCommandService;
import id.belajarbersama.application.identity.UserAdministrationService;
import id.belajarbersama.application.verification.VerificationReviewService;
import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.AdminUserPageResponse;
import id.belajarbersama.interfaces.rest.dto.AdminUserResponse;
import id.belajarbersama.interfaces.rest.dto.AssignCheckerRequest;
import id.belajarbersama.interfaces.rest.dto.AssignRoleRequest;
import id.belajarbersama.interfaces.rest.dto.ReviewDecisionRequest;
import id.belajarbersama.interfaces.rest.dto.TaxonomyCreateRequest;
import id.belajarbersama.interfaces.rest.dto.TaxonomyItemResponse;
import id.belajarbersama.interfaces.rest.dto.VerificationResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Admin")
public class AdminResource {
    private final VerificationReviewService reviews;
    private final UserAdministrationService users;
    private final ContentReviewService contentReviews;
    private final TaxonomyCommandService taxonomy;
    private final RequestAuthContext auth;

    public AdminResource(
            VerificationReviewService reviews,
            UserAdministrationService users,
            ContentReviewService contentReviews,
            TaxonomyCommandService taxonomy,
            RequestAuthContext auth) {
        this.reviews = reviews;
        this.users = users;
        this.contentReviews = contentReviews;
        this.taxonomy = taxonomy;
        this.auth = auth;
    }

    @GET
    @Path("/users")
    public AdminUserPageResponse listUsers(
            @QueryParam("q") String query,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        var result =
                users.list(
                        auth.requireUserId(),
                        query,
                        page == null ? 0 : page,
                        size == null ? 20 : size);
        return new AdminUserPageResponse(
                result.items().stream().map(AdminResource::toUser).toList(),
                result.page(),
                result.size(),
                result.totalItems());
    }

    @GET
    @Path("/users/{id}")
    public AdminUserResponse getUser(@PathParam("id") UUID id) {
        return toUser(users.view(auth.requireUserId(), UserId.of(id)));
    }

    @GET
    @Path("/verifications")
    public List<VerificationResponse> pending() {
        return reviews.listPending(auth.requireUserId()).stream()
                .map(item -> VerificationResource.toResponse(item, List.of()))
                .toList();
    }

    @GET
    @Path("/verifications/{id}")
    public VerificationResponse view(@PathParam("id") UUID id) {
        var detail = reviews.view(auth.requireUserId(), id);
        return VerificationResource.toResponse(detail.verification(), detail.evidence());
    }

    @POST
    @Path("/verifications/{id}/start-review")
    public VerificationResponse startReview(@PathParam("id") UUID id) {
        return VerificationResource.toResponse(
                reviews.startReview(auth.requireUserId(), id, auth.correlationId()), List.of());
    }

    @POST
    @Path("/verifications/{id}/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public VerificationResponse approve(@PathParam("id") UUID id, ReviewDecisionRequest request) {
        return VerificationResource.toResponse(
                reviews.approve(
                        auth.requireUserId(),
                        id,
                        request == null ? null : request.note(),
                        auth.correlationId()),
                List.of());
    }

    @POST
    @Path("/verifications/{id}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    public VerificationResponse reject(@PathParam("id") UUID id, ReviewDecisionRequest request) {
        return VerificationResource.toResponse(
                reviews.reject(
                        auth.requireUserId(),
                        id,
                        request == null ? null : request.note(),
                        auth.correlationId()),
                List.of());
    }

    @POST
    @Path("/verifications/{id}/request-changes")
    @Consumes(MediaType.APPLICATION_JSON)
    public VerificationResponse requestChanges(
            @PathParam("id") UUID id, ReviewDecisionRequest request) {
        return VerificationResource.toResponse(
                reviews.requestChanges(
                        auth.requireUserId(),
                        id,
                        request == null ? null : request.note(),
                        auth.correlationId()),
                List.of());
    }

    @POST
    @Path("/verifications/{id}/revoke")
    @Consumes(MediaType.APPLICATION_JSON)
    public VerificationResponse revoke(@PathParam("id") UUID id, ReviewDecisionRequest request) {
        return VerificationResource.toResponse(
                reviews.revoke(
                        auth.requireUserId(),
                        id,
                        request == null ? null : request.note(),
                        auth.correlationId()),
                List.of());
    }

    @POST
    @Path("/users/{id}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assignRole(@PathParam("id") UUID id, AssignRoleRequest request) {
        if (request == null || request.role() == null) {
            throw new ValidationException("role is required.");
        }
        users.assignRole(
                auth.requireUserId(),
                UserId.of(id),
                parseRole(request.role()),
                auth.correlationId());
        return Response.noContent().build();
    }

    @DELETE
    @Path("/users/{id}/roles/{role}")
    public Response revokeRole(@PathParam("id") UUID id, @PathParam("role") String role) {
        users.revokeRole(
                auth.requireUserId(), UserId.of(id), parseRole(role), auth.correlationId());
        return Response.noContent().build();
    }

    @POST
    @Path("/users/{id}/suspend")
    public Response suspend(@PathParam("id") UUID id) {
        users.suspend(auth.requireUserId(), UserId.of(id), auth.correlationId());
        return Response.noContent().build();
    }

    @POST
    @Path("/users/{id}/reactivate")
    public Response reactivate(@PathParam("id") UUID id) {
        users.reactivate(auth.requireUserId(), UserId.of(id), auth.correlationId());
        return Response.noContent().build();
    }

    @POST
    @Path("/users/{id}/deactivate")
    public Response deactivate(@PathParam("id") UUID id) {
        users.deactivate(auth.requireUserId(), UserId.of(id), auth.correlationId());
        return Response.noContent().build();
    }

    @POST
    @Path("/content/{id}/assign-reviewer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assignReviewer(@PathParam("id") UUID id, AssignCheckerRequest request) {
        if (request == null || request.checkerId() == null) {
            throw new ValidationException("checkerId is required.");
        }
        contentReviews.assign(
                auth.requireUserId(), id, UserId.of(request.checkerId()), auth.correlationId());
        return Response.noContent().build();
    }

    @POST
    @Path("/subjects")
    @Consumes(MediaType.APPLICATION_JSON)
    public TaxonomyItemResponse createSubject(TaxonomyCreateRequest request) {
        var subject =
                taxonomy.createSubject(
                        auth.requireUserId(),
                        request == null ? null : request.name(),
                        request == null ? null : request.description());
        return new TaxonomyItemResponse(
                subject.id(), subject.slug(), subject.name(), subject.description());
    }

    @POST
    @Path("/education-levels")
    @Consumes(MediaType.APPLICATION_JSON)
    public TaxonomyItemResponse createLevel(TaxonomyCreateRequest request) {
        var level =
                taxonomy.createLevel(
                        auth.requireUserId(),
                        request == null ? null : request.name(),
                        request == null || request.sortOrder() == null ? 100 : request.sortOrder());
        return new TaxonomyItemResponse(level.id(), level.slug(), level.name(), null);
    }

    private static AdminUserResponse toUser(UserAdministrationService.AdminUser user) {
        return new AdminUserResponse(
                user.id().value(),
                user.displayName(),
                user.status().name(),
                user.createdAt(),
                user.storedRoles().stream().map(Role::name).sorted().toList());
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ValidationException("Unknown role.");
        }
    }
}
