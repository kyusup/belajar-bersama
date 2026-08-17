package id.belajarbersama.domain.identity;

import java.time.Instant;

public record OauthState(
        String state,
        IdentityProviderId provider,
        String codeVerifier,
        String nonce,
        Instant expiresAt) {}
