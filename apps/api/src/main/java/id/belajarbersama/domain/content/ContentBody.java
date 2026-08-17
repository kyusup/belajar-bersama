package id.belajarbersama.domain.content;

import java.util.List;

public record ContentBody(List<ContentBlock> blocks) {
    public static ContentBody empty() {
        return new ContentBody(List.of());
    }
}
