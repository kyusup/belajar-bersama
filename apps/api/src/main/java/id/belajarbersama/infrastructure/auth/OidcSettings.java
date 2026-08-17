package id.belajarbersama.infrastructure.auth;

import id.belajarbersama.domain.identity.IdentityProviderId;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OidcSettings {
    private final Config config;
    private final String apiPublicUrl;
    private final String appUrl;
    private final boolean devLoginEnabled;

    public OidcSettings(
            Config config,
            @ConfigProperty(name = "bb.auth.api-public-url", defaultValue = "http://localhost:8080")
                    String apiPublicUrl,
            @ConfigProperty(name = "bb.auth.app-url", defaultValue = "http://localhost:3000")
                    String appUrl,
            @ConfigProperty(name = "bb.auth.dev-login.enabled", defaultValue = "false")
                    boolean devLoginEnabled) {
        this.config = config;
        this.apiPublicUrl = apiPublicUrl;
        this.appUrl = appUrl;
        this.devLoginEnabled = devLoginEnabled;
    }

    public boolean googleEnabled() {
        return isConfigured(googleClientId()) && isConfigured(googleClientSecret());
    }

    public boolean appleEnabled() {
        return isConfigured(appleClientId())
                && isConfigured(appleTeamId())
                && isConfigured(appleKeyId())
                && isConfigured(applePrivateKey());
    }

    public boolean isEnabled(IdentityProviderId provider) {
        return provider == IdentityProviderId.GOOGLE ? googleEnabled() : appleEnabled();
    }

    public String googleClientId() {
        return value("bb.auth.google.client-id");
    }

    public String googleClientSecret() {
        return value("bb.auth.google.client-secret");
    }

    public String appleClientId() {
        return value("bb.auth.apple.client-id");
    }

    public String appleTeamId() {
        return value("bb.auth.apple.team-id");
    }

    public String appleKeyId() {
        return value("bb.auth.apple.key-id");
    }

    public String applePrivateKey() {
        return value("bb.auth.apple.private-key");
    }

    public String callbackUrl(IdentityProviderId provider) {
        return apiPublicUrl.replaceAll("/$", "")
                + "/api/v1/auth/"
                + provider.name().toLowerCase()
                + "/callback";
    }

    public String appUrl() {
        return appUrl;
    }

    public boolean devLoginEnabled() {
        return devLoginEnabled;
    }

    private String value(String name) {
        String raw = config.getConfigValue(name).getValue();
        return raw == null ? "" : raw.trim();
    }

    private static boolean isConfigured(String value) {
        return !value.isBlank() && !"unset".equalsIgnoreCase(value);
    }
}
