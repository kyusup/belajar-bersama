package id.belajarbersama.infrastructure.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.identity.ExternalIdentityClaims;
import id.belajarbersama.domain.identity.IdentityProviderId;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * OIDC authorization-code + ID-token validation for Google and Apple. Tokens are never persisted.
 */
@ApplicationScoped
public class OidcIdentityGateway {
    private static final String GOOGLE_AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String APPLE_AUTH = "https://appleid.apple.com/auth/authorize";
    private static final String APPLE_TOKEN = "https://appleid.apple.com/auth/token";
    private static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final OidcSettings settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OidcIdentityGateway(OidcSettings settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
    }

    public String authorizationUrl(
            IdentityProviderId provider, String state, String nonce, String codeChallenge) {
        if (!settings.isEnabled(provider)) {
            throw new AuthorizationException(
                    ErrorCodes.AUTH_PROVIDER_NOT_CONFIGURED,
                    "This identity provider is not configured.");
        }
        if (provider == IdentityProviderId.GOOGLE) {
            return GOOGLE_AUTH
                    + "?client_id="
                    + enc(settings.googleClientId())
                    + "&redirect_uri="
                    + enc(settings.callbackUrl(provider))
                    + "&response_type=code&scope="
                    + enc("openid profile")
                    + "&state="
                    + enc(state)
                    + "&nonce="
                    + enc(nonce)
                    + "&code_challenge="
                    + enc(codeChallenge)
                    + "&code_challenge_method=S256";
        }
        return APPLE_AUTH
                + "?client_id="
                + enc(settings.appleClientId())
                + "&redirect_uri="
                + enc(settings.callbackUrl(provider))
                + "&response_type=code&response_mode=query&scope="
                + enc("openid name")
                + "&state="
                + enc(state)
                + "&nonce="
                + enc(nonce)
                + "&code_challenge="
                + enc(codeChallenge)
                + "&code_challenge_method=S256";
    }

    public ExternalIdentityClaims exchange(
            IdentityProviderId provider, String code, String codeVerifier, String nonce) {
        try {
            String clientSecret =
                    provider == IdentityProviderId.GOOGLE
                            ? settings.googleClientSecret()
                            : AppleClientSecret.generate(settings);
            String tokenUrl = provider == IdentityProviderId.GOOGLE ? GOOGLE_TOKEN : APPLE_TOKEN;
            String clientId =
                    provider == IdentityProviderId.GOOGLE
                            ? settings.googleClientId()
                            : settings.appleClientId();
            String body =
                    "grant_type=authorization_code&code="
                            + enc(code)
                            + "&client_id="
                            + enc(clientId)
                            + "&client_secret="
                            + enc(clientSecret)
                            + "&redirect_uri="
                            + enc(settings.callbackUrl(provider))
                            + "&code_verifier="
                            + enc(codeVerifier);
            HttpRequest request =
                    HttpRequest.newBuilder(URI.create(tokenUrl))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .timeout(Duration.ofSeconds(20))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AuthorizationException(
                        ErrorCodes.AUTH_INVALID_IDENTITY, "Identity provider rejected the login.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            String idToken = json.path("id_token").asText(null);
            if (idToken == null || idToken.isBlank()) {
                throw new AuthorizationException(
                        ErrorCodes.AUTH_INVALID_IDENTITY, "Identity token was missing.");
            }
            return parseIdToken(provider, idToken, nonce);
        } catch (AuthorizationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthorizationException(
                    ErrorCodes.AUTH_INVALID_IDENTITY,
                    "Could not validate the identity provider response.");
        }
    }

    ExternalIdentityClaims parseIdToken(
            IdentityProviderId provider, String idToken, String expectedNonce) throws Exception {
        String jwks = provider == IdentityProviderId.GOOGLE ? GOOGLE_JWKS : APPLE_JWKS;
        String issuer = provider == IdentityProviderId.GOOGLE ? GOOGLE_ISSUER : APPLE_ISSUER;
        String audience =
                provider == IdentityProviderId.GOOGLE
                        ? settings.googleClientId()
                        : settings.appleClientId();
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(URI.create(jwks).toURL());
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(
                        Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256), keySource));
        JWTClaimsSet claims = processor.process(idToken, null);
        if (!issuer.equals(claims.getIssuer()) || !claims.getAudience().contains(audience)) {
            throw new AuthorizationException(
                    ErrorCodes.AUTH_INVALID_IDENTITY,
                    "Identity token issuer or audience is invalid.");
        }
        if (expectedNonce != null && !expectedNonce.equals(claims.getStringClaim("nonce"))) {
            throw new AuthorizationException(
                    ErrorCodes.AUTH_INVALID_IDENTITY, "Identity token nonce is invalid.");
        }
        String name = claims.getStringClaim("name");
        if (name == null || name.isBlank()) {
            name = claims.getStringClaim("email");
        }
        return new ExternalIdentityClaims(
                provider, issuer, claims.getSubject(), name, claims.getStringClaim("picture"));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
