package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record CompetencyResponse(UUID id, String slug, String name, String description) {}
