package id.belajarbersama.interfaces.rest.dto;

import java.util.Map;

public record ApiErrorResponse(
        String code, String message, Map<String, Object> details, String correlationId) {}
