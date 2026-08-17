package id.belajarbersama.interfaces.http;

import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.security.RateLimiter;
import id.belajarbersama.infrastructure.security.AbuseControlSettings;
import id.belajarbersama.interfaces.rest.CorrelationIdFilter;
import id.belajarbersama.interfaces.rest.dto.ApiErrorResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Provider
@Priority(2500)
@ApplicationScoped
public class RateLimitFilter implements ContainerRequestFilter {
    private final RateLimiter limiter = new RateLimiter();
    private final AbuseControlSettings settings;
    private final RequestAuthContext auth;

    @Context HttpServerRequest vertxRequest;

    public RateLimitFilter(AbuseControlSettings settings, RequestAuthContext auth) {
        this.settings = settings;
        this.auth = auth;
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if (!settings.rateLimitEnabled()) {
            return;
        }
        String path = request.getUriInfo().getPath();
        if (path == null) {
            path = "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        RateLimiter.Bucket bucket = bucketOf(request.getMethod(), path);
        if (bucket == null) {
            return;
        }
        RateLimiter.Result result =
                limiter.allow(identity(request), bucket, settings.limit(bucket), Instant.now());
        if (result.allowed()) {
            return;
        }
        Object correlation = request.getProperty(CorrelationIdFilter.PROPERTY);
        request.abortWith(
                Response.status(429)
                        .type(MediaType.APPLICATION_JSON)
                        .header("Retry-After", String.valueOf(result.retryAfterSeconds()))
                        .header("Cache-Control", "private, no-store")
                        .entity(
                                new ApiErrorResponse(
                                        ErrorCodes.RATE_LIMITED,
                                        "Too many requests. Try again shortly.",
                                        Map.of("retryAfterSeconds", result.retryAfterSeconds()),
                                        correlation == null ? null : correlation.toString()))
                        .build());
    }

    private String identity(ContainerRequestContext request) {
        return auth.userId()
                .map(id -> "user:" + id.value())
                .orElseGet(() -> "ip:" + clientIp(request));
    }

    private String clientIp(ContainerRequestContext request) {
        if (settings.trustForwardedFor()) {
            String forwarded = request.getHeaderString("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        if (vertxRequest != null && vertxRequest.remoteAddress() != null) {
            return vertxRequest.remoteAddress().host();
        }
        return "unknown";
    }

    static RateLimiter.Bucket bucketOf(String method, String path) {
        String verb = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(verb)) {
            return null;
        }
        if (path.startsWith("/q/")
                || path.equals("/api/v1/health")
                || path.equals("/api/v1/status")
                || path.equals("/api/v1/auth/config")) {
            return null;
        }
        if (path.startsWith("/api/v1/auth/") && !"GET".equals(verb)) {
            return RateLimiter.Bucket.AUTH;
        }
        if (path.endsWith("/reports") && "POST".equals(verb)) {
            return RateLimiter.Bucket.REPORT;
        }
        if (path.equals("/api/v1/public/search")) {
            return RateLimiter.Bucket.SEARCH;
        }
        if ("POST".equals(verb)
                || "PUT".equals(verb)
                || "PATCH".equals(verb)
                || "DELETE".equals(verb)) {
            return RateLimiter.Bucket.WRITE;
        }
        if (path.startsWith("/api/v1/public/")) {
            return RateLimiter.Bucket.PUBLIC;
        }
        return null;
    }
}
