package id.belajarbersama.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.belajarbersama.domain.content.ContentBody;
import id.belajarbersama.domain.error.InfrastructureException;

final class ContentBodyJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContentBodyJson() {}

    static String write(ContentBody body) {
        try {
            return MAPPER.writeValueAsString(body == null ? ContentBody.empty() : body);
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to serialize content body", exception);
        }
    }

    static ContentBody read(String json) {
        if (json == null || json.isBlank()) {
            return ContentBody.empty();
        }
        try {
            return MAPPER.readValue(json, ContentBody.class);
        } catch (Exception exception) {
            throw new InfrastructureException("Failed to parse content body", exception);
        }
    }
}
