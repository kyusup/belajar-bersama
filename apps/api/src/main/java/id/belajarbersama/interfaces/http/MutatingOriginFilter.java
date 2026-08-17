package id.belajarbersama.interfaces.http;

import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.security.OriginAllowList;
import id.belajarbersama.infrastructure.security.AbuseControlSettings;
import id.belajarbersama.interfaces.rest.CorrelationIdFilter;
import id.belajarbersama.interfaces.rest.dto.ApiErrorResponse;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Provider
@Priority(1500)
@ApplicationScoped
public class MutatingOriginFilter implements ContainerRequestFilter {
    private final Set<String> allowed;

    public MutatingOriginFilter(AbuseControlSettings settings) {
        this.allowed = OriginAllowList.parse(settings.corsOrigins(), settings.appUrl());
    }

    @Override
    public void filter(ContainerRequestContext request) {
        String method =
                request.getMethod() == null ? "GET" : request.getMethod().toUpperCase(Locale.ROOT);
        if (!isMutating(method)) {
            return;
        }
        String path = request.getUriInfo().getPath();
        if (path != null && (path.startsWith("q/") || path.startsWith("/q/"))) {
            return;
        }
        String origin = request.getHeaderString("Origin");
        String referer = request.getHeaderString("Referer");
        if ((origin == null || origin.isBlank()) && (referer == null || referer.isBlank())) {
            return;
        }
        String candidate = origin == null || origin.isBlank() ? referer : origin;
        if (OriginAllowList.allowed(candidate, allowed)) {
            return;
        }
        Object correlation = request.getProperty(CorrelationIdFilter.PROPERTY);
        request.abortWith(
                Response.status(403)
                        .type(MediaType.APPLICATION_JSON)
                        .header("Cache-Control", "private, no-store")
                        .entity(
                                new ApiErrorResponse(
                                        ErrorCodes.CSRF_ORIGIN_DENIED,
                                        "Cross-origin mutating requests are not allowed.",
                                        Map.of(),
                                        correlation == null ? null : correlation.toString()))
                        .build());
    }

    private static boolean isMutating(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
    }
}
