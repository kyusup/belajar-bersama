package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.identity.AuthenticateExternalIdentityService;
import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.application.identity.SessionService;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.ExternalIdentityClaims;
import id.belajarbersama.domain.identity.Identity;
import id.belajarbersama.domain.identity.IdentityProviderId;
import id.belajarbersama.domain.identity.OauthState;
import id.belajarbersama.domain.identity.OauthStateRepository;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.infrastructure.auth.OidcIdentityGateway;
import id.belajarbersama.infrastructure.auth.OidcSettings;
import id.belajarbersama.infrastructure.auth.Pkce;
import id.belajarbersama.infrastructure.auth.SessionCookies;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.AuthConfigResponse;
import id.belajarbersama.interfaces.rest.dto.DevLoginRequest;
import id.belajarbersama.interfaces.rest.dto.IdentityResponse;
import id.belajarbersama.interfaces.rest.dto.MeResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Identity")
public class AuthResource {
    private final OidcSettings settings;
    private final OidcIdentityGateway oidc;
    private final OauthStateRepository oauthStates;
    private final AuthenticateExternalIdentityService authenticate;
    private final SessionService sessions;
    private final CurrentUserQuery currentUserQuery;
    private final RequestAuthContext requestAuthContext;
    private final boolean cookieSecure;

    public AuthResource(
            OidcSettings settings,
            OidcIdentityGateway oidc,
            OauthStateRepository oauthStates,
            AuthenticateExternalIdentityService authenticate,
            SessionService sessions,
            CurrentUserQuery currentUserQuery,
            RequestAuthContext requestAuthContext,
            @ConfigProperty(name = "bb.auth.cookie-secure", defaultValue = "false")
                    boolean cookieSecure) {
        this.settings = settings;
        this.oidc = oidc;
        this.oauthStates = oauthStates;
        this.authenticate = authenticate;
        this.sessions = sessions;
        this.currentUserQuery = currentUserQuery;
        this.requestAuthContext = requestAuthContext;
        this.cookieSecure = cookieSecure;
    }

    @GET
    @Path("/auth/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(
                settings.googleEnabled(), settings.appleEnabled(), settings.devLoginEnabled());
    }

    @GET
    @Path("/auth/{provider}/start")
    public Response start(@PathParam("provider") String providerValue) {
        IdentityProviderId provider = parseProvider(providerValue);
        String state = Pkce.randomUrl(24);
        String verifier = Pkce.randomUrl(32);
        String nonce = Pkce.randomUrl(24);
        oauthStates.save(
                new OauthState(state, provider, verifier, nonce, Instant.now().plusSeconds(600)));
        String url = oidc.authorizationUrl(provider, state, nonce, Pkce.challengeS256(verifier));
        return Response.seeOther(URI.create(url)).build();
    }

    @GET
    @Path("/auth/{provider}/callback")
    public Response callback(
            @PathParam("provider") String providerValue,
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String error) {
        if (error != null && !error.isBlank()) {
            return Response.seeOther(URI.create(settings.appUrl() + "/masuk?error=denied")).build();
        }
        IdentityProviderId provider = parseProvider(providerValue);
        OauthState stored =
                oauthStates
                        .consume(state)
                        .filter(item -> item.provider() == provider)
                        .filter(item -> item.expiresAt().isAfter(Instant.now()))
                        .orElseThrow(
                                () ->
                                        new AuthorizationException(
                                                ErrorCodes.AUTH_INVALID_IDENTITY,
                                                "Login state is invalid or expired."));
        ExternalIdentityClaims claims =
                oidc.exchange(provider, code, stored.codeVerifier(), stored.nonce());
        User user = authenticate.authenticate(claims, requestAuthContext.correlationId());
        return withSession(user, Response.seeOther(URI.create(settings.appUrl() + "/akun")));
    }

    @POST
    @Path("/auth/dev/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response devLogin(DevLoginRequest request) {
        if (!settings.devLoginEnabled()) {
            throw new AuthorizationException(
                    ErrorCodes.DEV_LOGIN_DISABLED, "Development login is disabled.");
        }
        if (request == null || request.subject() == null || request.subject().isBlank()) {
            throw new ValidationException("subject is required.");
        }
        IdentityProviderId provider = parseProvider(request.provider());
        String issuer =
                provider == IdentityProviderId.GOOGLE
                        ? "https://accounts.google.com"
                        : "https://appleid.apple.com";
        User user =
                authenticate.authenticate(
                        new ExternalIdentityClaims(
                                provider,
                                issuer,
                                request.subject().trim(),
                                request.displayName(),
                                request.avatarUrl()),
                        requestAuthContext.correlationId());
        return withSession(user, Response.ok(toMe(currentUserQuery.load(user.id()))));
    }

    @POST
    @Path("/auth/logout")
    public Response logout() {
        sessions.revoke(requestAuthContext.sessionToken());
        return Response.noContent().cookie(SessionCookies.clear(cookieSecure, "/")).build();
    }

    @GET
    @Path("/me")
    public MeResponse me() {
        return toMe(currentUserQuery.load(requestAuthContext.requireUserId()));
    }

    @GET
    @Path("/me/identities")
    public java.util.List<IdentityResponse> identities() {
        return toMe(currentUserQuery.load(requestAuthContext.requireUserId())).identities();
    }

    @GET
    @Path("/me/roles")
    public Set<String> roles() {
        return toMe(currentUserQuery.load(requestAuthContext.requireUserId())).roles();
    }

    @GET
    @Path("/me/permissions")
    public Set<String> permissions() {
        return toMe(currentUserQuery.load(requestAuthContext.requireUserId())).permissions();
    }

    private Response withSession(User user, Response.ResponseBuilder builder) {
        SessionService.IssuedSession issued = sessions.issue(user.id());
        NewCookie cookie = SessionCookies.create(issued.token(), sessions.ttl(), cookieSecure, "/");
        return builder.cookie(cookie).build();
    }

    static MeResponse toMe(CurrentUserQuery.CurrentUserView view) {
        return new MeResponse(
                view.user().id().value(),
                view.user().displayName(),
                view.user().avatarUrl(),
                view.user().status().name(),
                view.effectiveRoles().stream().map(Role::name).collect(Collectors.toSet()),
                view.storedRoles().stream().map(Role::name).collect(Collectors.toSet()),
                view.permissions().stream().map(Permission::name).collect(Collectors.toSet()),
                view.identities().stream().map(AuthResource::toIdentity).toList(),
                view.approvedCompetencyIds());
    }

    private static IdentityResponse toIdentity(Identity identity) {
        return new IdentityResponse(identity.id(), identity.provider().name(), identity.issuer());
    }

    private static IdentityProviderId parseProvider(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("provider is required.");
        }
        try {
            return IdentityProviderId.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Unsupported identity provider.");
        }
    }
}
