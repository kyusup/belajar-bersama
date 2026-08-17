package id.belajarbersama.infrastructure.security;

import id.belajarbersama.domain.security.RateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AbuseControlSettings {
    private final boolean rateLimitEnabled;
    private final boolean trustForwardedFor;
    private final int authPerMinute;
    private final int writePerMinute;
    private final int reportPerMinute;
    private final int searchPerMinute;
    private final int publicPerMinute;
    private final String corsOrigins;
    private final String appUrl;

    public AbuseControlSettings(
            @ConfigProperty(name = "bb.rate-limit.enabled", defaultValue = "true")
                    boolean rateLimitEnabled,
            @ConfigProperty(name = "bb.rate-limit.trust-forwarded-for", defaultValue = "false")
                    boolean trustForwardedFor,
            @ConfigProperty(name = "bb.rate-limit.auth-per-minute", defaultValue = "20")
                    int authPerMinute,
            @ConfigProperty(name = "bb.rate-limit.write-per-minute", defaultValue = "60")
                    int writePerMinute,
            @ConfigProperty(name = "bb.rate-limit.report-per-minute", defaultValue = "10")
                    int reportPerMinute,
            @ConfigProperty(name = "bb.rate-limit.search-per-minute", defaultValue = "40")
                    int searchPerMinute,
            @ConfigProperty(name = "bb.rate-limit.public-per-minute", defaultValue = "120")
                    int publicPerMinute,
            @ConfigProperty(
                            name = "quarkus.http.cors.origins",
                            defaultValue = "http://localhost:3000")
                    String corsOrigins,
            @ConfigProperty(name = "bb.auth.app-url", defaultValue = "http://localhost:3000")
                    String appUrl) {
        this.rateLimitEnabled = rateLimitEnabled;
        this.trustForwardedFor = trustForwardedFor;
        this.authPerMinute = authPerMinute;
        this.writePerMinute = writePerMinute;
        this.reportPerMinute = reportPerMinute;
        this.searchPerMinute = searchPerMinute;
        this.publicPerMinute = publicPerMinute;
        this.corsOrigins = corsOrigins;
        this.appUrl = appUrl;
    }

    public boolean rateLimitEnabled() {
        return rateLimitEnabled;
    }

    public boolean trustForwardedFor() {
        return trustForwardedFor;
    }

    public int limit(RateLimiter.Bucket bucket) {
        return switch (bucket) {
            case AUTH -> authPerMinute;
            case WRITE -> writePerMinute;
            case REPORT -> reportPerMinute;
            case SEARCH -> searchPerMinute;
            case PUBLIC -> publicPerMinute;
        };
    }

    public String corsOrigins() {
        return corsOrigins;
    }

    public String appUrl() {
        return appUrl;
    }
}
