package id.belajarbersama.interfaces.rest.dto;

public record EvidenceRequest(
        String kind, String summary, String referenceUrl, String storageKey) {}
