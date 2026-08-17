package id.belajarbersama.interfaces.http;

import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.infrastructure.auth.SessionCookies;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Optional;

@RequestScoped
public class RequestAuthContext {
    private UserId userId;
    private String correlationId;
    private String sessionToken;

    public void bind(UserId userId, String sessionToken, String correlationId) {
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.correlationId = correlationId;
    }

    public Optional<UserId> userId() {
        return Optional.ofNullable(userId);
    }

    public UserId requireUserId() {
        return userId().orElseThrow(
                        () ->
                                new id.belajarbersama.domain.error.AuthorizationException(
                                        id.belajarbersama.domain.error.ErrorCodes.UNAUTHENTICATED,
                                        "Authentication is required."));
    }

    public String correlationId() {
        return correlationId;
    }

    public String sessionToken() {
        return sessionToken;
    }

    public static String cookieToken(ContainerRequestContext request) {
        if (request.getCookies() == null) {
            return null;
        }
        var cookie = request.getCookies().get(SessionCookies.NAME);
        return cookie == null ? null : cookie.getValue();
    }
}
