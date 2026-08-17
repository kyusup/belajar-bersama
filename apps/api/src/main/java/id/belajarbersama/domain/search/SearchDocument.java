package id.belajarbersama.domain.search;

import java.util.Map;

public record SearchDocument(
        String id, String type, String title, String body, Map<String, String> fields) {}
