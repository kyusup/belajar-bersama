package id.belajarbersama.domain.identity;

/**
 * Validated claims from an identity provider. Produced by infrastructure after signature/issuer
 * checks. Domain services must not parse raw OAuth tokens.
 */
public record ExternalIdentityClaims(
        IdentityProviderId provider,
        String issuer,
        String subject,
        String displayName,
        String avatarUrl) {
    public ExternalIdentityClaims {
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
    }
}
