package id.belajarbersama.domain.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Untrusted contributor body → structured blocks with no HTML/scripts. */
public final class ContentSanitizer {
    private static final Set<String> TYPES =
            Set.of("heading", "paragraph", "list", "code", "quote", "link", "image");
    private static final int MAX_TEXT = 20_000;
    private static final int MAX_BLOCKS = 200;

    private ContentSanitizer() {}

    public static ContentBody sanitize(ContentBody body) {
        if (body == null || body.blocks() == null) {
            return ContentBody.empty();
        }
        List<ContentBlock> cleaned = new ArrayList<>();
        int count = 0;
        for (ContentBlock block : body.blocks()) {
            if (block == null || count >= MAX_BLOCKS) {
                continue;
            }
            String type = block.type() == null ? "" : block.type().trim().toLowerCase(Locale.ROOT);
            if (!TYPES.contains(type)) {
                continue;
            }
            cleaned.add(sanitizeBlock(type, block));
            count++;
        }
        return new ContentBody(List.copyOf(cleaned));
    }

    public static String plainText(ContentBody body) {
        ContentBody safe = sanitize(body);
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : safe.blocks()) {
            if (block.text() != null && !block.text().isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(block.text());
            }
            if (block.items() != null) {
                for (String item : block.items()) {
                    if (item != null && !item.isBlank()) {
                        if (!builder.isEmpty()) {
                            builder.append('\n');
                        }
                        builder.append(item);
                    }
                }
            }
        }
        return builder.toString();
    }

    public static boolean hasSubstance(ContentBody body) {
        return !plainText(body).isBlank();
    }

    public static String plainText(String raw) {
        if (raw == null) {
            return "";
        }
        return stripMarkup(raw).trim();
    }

    public static String safeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("https://") || lower.startsWith("http://"))) {
            return null;
        }
        if (lower.contains("javascript:") || lower.contains("data:")) {
            return null;
        }
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }

    private static ContentBlock sanitizeBlock(String type, ContentBlock block) {
        Integer level = block.level();
        if (level != null) {
            level = Math.min(3, Math.max(1, level));
        }
        List<String> items = null;
        if (block.items() != null) {
            items =
                    block.items().stream()
                            .map(ContentSanitizer::plainText)
                            .filter(item -> !item.isBlank())
                            .limit(50)
                            .toList();
        }
        String href = null;
        if ("link".equals(type) || "image".equals(type)) {
            href = safeHttpUrl(block.href());
        }
        return new ContentBlock(
                type,
                level,
                clip(plainText(block.text())),
                Boolean.TRUE.equals(block.ordered()),
                items,
                clip(plainText(block.language()), 32),
                href);
    }

    private static String stripMarkup(String raw) {
        String withoutTags = raw.replaceAll("(?is)<script.*?>.*?</script>", " ");
        withoutTags = withoutTags.replaceAll("(?is)<style.*?>.*?</style>", " ");
        withoutTags = withoutTags.replaceAll("<[^>]+>", " ");
        withoutTags = withoutTags.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        withoutTags = withoutTags.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        return withoutTags.replaceAll("\\s+", " ");
    }

    private static String clip(String value) {
        return clip(value, MAX_TEXT);
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
