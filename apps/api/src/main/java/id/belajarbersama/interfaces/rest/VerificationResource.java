package id.belajarbersama.interfaces.rest;

import id.belajarbersama.application.verification.EvidenceInput;
import id.belajarbersama.application.verification.VerificationApplicationService;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.verification.Verification;
import id.belajarbersama.domain.verification.VerificationEvidence;
import id.belajarbersama.interfaces.http.RequestAuthContext;
import id.belajarbersama.interfaces.rest.dto.EvidenceResponse;
import id.belajarbersama.interfaces.rest.dto.SubmitVerificationRequest;
import id.belajarbersama.interfaces.rest.dto.VerificationResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/verifications")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Verification")
public class VerificationResource {
    private final VerificationApplicationService applications;
    private final RequestAuthContext auth;

    public VerificationResource(
            VerificationApplicationService applications, RequestAuthContext auth) {
        this.applications = applications;
        this.auth = auth;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public VerificationResponse submit(SubmitVerificationRequest request) {
        if (request == null || request.competencyId() == null) {
            throw new ValidationException("competencyId is required.");
        }
        List<EvidenceInput> evidence =
                request.evidence() == null
                        ? List.of()
                        : request.evidence().stream()
                                .map(
                                        item ->
                                                new EvidenceInput(
                                                        item.kind(),
                                                        item.summary(),
                                                        item.referenceUrl(),
                                                        item.storageKey()))
                                .toList();
        Verification saved =
                applications.submit(
                        auth.requireUserId(),
                        request.competencyId(),
                        request.qualification(),
                        request.experience(),
                        evidence,
                        auth.correlationId());
        return toResponse(saved, List.of());
    }

    @GET
    @Path("/me")
    public List<VerificationResponse> mine() {
        return applications.listMine(auth.requireUserId()).stream()
                .map(item -> toResponse(item, List.of()))
                .toList();
    }

    static VerificationResponse toResponse(
            Verification verification, List<VerificationEvidence> evidence) {
        return new VerificationResponse(
                verification.id(),
                verification.applicantId().value(),
                verification.competencyId(),
                verification.status().name(),
                verification.qualification(),
                verification.experience(),
                verification.reviewerId() == null ? null : verification.reviewerId().value(),
                verification.decisionNote(),
                verification.decidedAt(),
                verification.createdAt(),
                verification.updatedAt(),
                evidence.stream()
                        .map(
                                item ->
                                        new EvidenceResponse(
                                                item.id(),
                                                item.kind(),
                                                item.summary(),
                                                item.referenceUrl(),
                                                item.storageKey()))
                        .toList());
    }
}
