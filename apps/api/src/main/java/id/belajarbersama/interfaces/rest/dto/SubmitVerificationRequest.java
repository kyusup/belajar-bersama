package id.belajarbersama.interfaces.rest.dto;

import java.util.List;
import java.util.UUID;

public record SubmitVerificationRequest(
        UUID competencyId,
        String qualification,
        String experience,
        List<EvidenceRequest> evidence) {}
