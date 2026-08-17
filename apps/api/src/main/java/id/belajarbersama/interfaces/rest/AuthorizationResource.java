package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.authorization.AuthorizationQueryService;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.EligibilityResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/authorization")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authorization")
public class AuthorizationResource {
    private final AuthorizationQueryService authorization;
    private final RequestAuthContext auth;

    public AuthorizationResource(AuthorizationQueryService authorization, RequestAuthContext auth) {
        this.authorization = authorization;
        this.auth = auth;
    }

    @GET
    @Path("/can-create-content")
    public EligibilityResponse canCreate(@QueryParam("competencyId") UUID competencyId) {
        return new EligibilityResponse(
                authorization.canCreateContent(auth.requireUserId(), competencyId));
    }

    @GET
    @Path("/can-review")
    public EligibilityResponse canReview(
            @QueryParam("competencyId") UUID competencyId, @QueryParam("makerId") UUID makerId) {
        return new EligibilityResponse(
                authorization.canReview(
                        auth.requireUserId(),
                        competencyId,
                        makerId == null ? null : UserId.of(makerId)));
    }
}
