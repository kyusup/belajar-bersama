package id.belajarbersama.interfaces.rest.dto;

import java.util.UUID;

public record EvidenceResponse(
        UUID id, String kind, String summary, String referenceUrl, String storageKey) {}
