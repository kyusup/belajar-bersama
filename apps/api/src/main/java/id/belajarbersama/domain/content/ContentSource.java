package id.belajarbersama.domain.content;

import java.util.UUID;

public record ContentSource(
        UUID id,
        String title,
        String author,
        String publisher,
        String url,
        String publicationInfo,
        String notes,
        int sortOrder) {}
