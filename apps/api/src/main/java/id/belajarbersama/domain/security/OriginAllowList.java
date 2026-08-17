package id.belajarbersama.domain.security;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Browser Origin / Referer checks for cookie-authenticated mutating requests. */
public final class OriginAllowList {
    private OriginAllowList() {}

    public static Set<String> parse(String... raw) {
        return Arrays.stream(raw == null ? new String[0] : raw)
                .filter(item -> item != null && !item.isBlank())
                .flatMap(item -> Arrays.stream(item.split(",")))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(OriginAllowList::normalize)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean allowed(String originOrReferer, Set<String> allowed) {
        if (originOrReferer == null || originOrReferer.isBlank()) {
            return true;
        }
        String origin = extractOrigin(originOrReferer);
        if (origin.isBlank()) {
            return false;
        }
        return allowed.contains(origin);
    }

    public static String extractOrigin(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed.contains("://") ? trimmed : "https://" + trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return "";
            }
            int port = uri.getPort();
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (port == -1) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (Exception exception) {
            return "";
        }
    }

    private static String normalize(String origin) {
        return extractOrigin(origin);
    }
}
