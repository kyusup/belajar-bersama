package id.belajarbersama.domain.storage;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Generated object key. Never use a raw user-supplied filename as the storage key. */
public record ObjectKey(String value) {
    private static final Pattern SAFE = Pattern.compile("^[a-zA-Z0-9._/-]{1,512}$");

    public ObjectKey {
        Objects.requireNonNull(value, "Object key is required");
        if (!SAFE.matcher(value).matches() || value.contains("..")) {
            throw new IllegalArgumentException("Invalid object key");
        }
    }

    public static ObjectKey of(String prefix, String generatedName) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT).replaceAll("^/+|/+$", "");
        return new ObjectKey(normalizedPrefix + "/" + generatedName);
    }
}
