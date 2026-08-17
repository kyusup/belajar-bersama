package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record ReportResponse(UUID id, UUID contentId, String status) {}
