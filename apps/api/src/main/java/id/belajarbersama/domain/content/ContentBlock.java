package id.belajarbersama.domain.content;

import java.util.List;

public record ContentBlock(
        String type,
        Integer level,
        String text,
        Boolean ordered,
        List<String> items,
        String language,
        String href) {}
