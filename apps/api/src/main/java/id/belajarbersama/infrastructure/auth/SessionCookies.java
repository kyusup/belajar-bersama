package id.belajarbersama.infrastructure.auth;

import jakarta.ws.rs.core.NewCookie;
import java.time.Duration;

public final class SessionCookies {
    public static final String NAME = "bb_session";

    private SessionCookies() {}

    public static NewCookie create(String token, Duration ttl, boolean secure, String path) {
        return new NewCookie.Builder(NAME)
                .value(token)
                .path(path)
                .maxAge((int) ttl.toSeconds())
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    public static NewCookie clear(boolean secure, String path) {
        return new NewCookie.Builder(NAME)
                .value("")
                .path(path)
                .maxAge(0)
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }
}
