package id.belajarbersama.interfaces.http;

import id.belajarbersama.application.identity.SessionService;
import id.belajarbersama.interfaces.rest.CorrelationIdFilter;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(2000)
public class SessionAuthFilter implements ContainerRequestFilter {
    @Inject SessionService sessionService;
    @Inject RequestAuthContext requestAuthContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Object correlation = requestContext.getProperty(CorrelationIdFilter.PROPERTY);
        String correlationId = correlation == null ? null : correlation.toString();
        String token = RequestAuthContext.cookieToken(requestContext);
        sessionService
                .resolve(token)
                .ifPresent(
                        session -> requestAuthContext.bind(session.userId(), token, correlationId));
        if (requestAuthContext.userId().isEmpty()) {
            requestAuthContext.bind(null, token, correlationId);
        }
    }
}
