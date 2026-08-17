package id.belajarbersama.domain.content;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private Slugs() {}

    public static String fromTitle(String title) {
        if (title == null || title.isBlank()) {
            return "materi";
        }
        String normalized =
                Normalizer.normalize(title, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .toLowerCase(Locale.ROOT);
        String slug = NON_SLUG.matcher(normalized).replaceAll("-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        if (slug.isBlank()) {
            slug = "materi";
        }
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-+$", "") : slug;
    }
}
