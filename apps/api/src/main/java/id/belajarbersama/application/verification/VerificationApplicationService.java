package id.belajarbersama.application.verification;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.competency.CompetencyRepository;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.verification.Verification;
import id.belajarbersama.domain.verification.VerificationEvidence;
import id.belajarbersama.domain.verification.VerificationPolicy;
import id.belajarbersama.domain.verification.VerificationRepository;
import id.belajarbersama.domain.verification.VerificationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class VerificationApplicationService {
    private final CurrentUserQuery currentUserQuery;
    private final CompetencyRepository competencies;
    private final VerificationRepository verifications;
    private final AuditRecorder auditRecorder;

    public VerificationApplicationService(
            CurrentUserQuery currentUserQuery,
            CompetencyRepository competencies,
            VerificationRepository verifications,
            AuditRecorder auditRecorder) {
        this.currentUserQuery = currentUserQuery;
        this.competencies = competencies;
        this.verifications = verifications;
        this.auditRecorder = auditRecorder;
    }

    public Verification submit(
            UserId actorId,
            UUID competencyId,
            String qualification,
            String experience,
            List<EvidenceInput> evidence,
            String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.VERIFICATION_APPLY);
        competencies
                .findById(competencyId)
                .filter(competency -> competency.active())
                .orElseThrow(() -> new NotFoundException("Competency not found."));
        if (qualification == null || qualification.isBlank()) {
            throw new ValidationException("Qualification information is required.");
        }
        if (verifications.findApproved(actorId, competencyId).isPresent()) {
            throw new ConflictException(
                    ErrorCodes.CONFLICT,
                    "An approved verification already exists for this competency.");
        }
        Instant now = Instant.now();
        Verification existingOpen =
                verifications.listByApplicant(actorId).stream()
                        .filter(item -> item.competencyId().equals(competencyId) && item.isOpen())
                        .findFirst()
                        .orElse(null);
        Verification saved;
        if (existingOpen != null) {
            saved =
                    VerificationPolicy.resubmit(
                            new Verification(
                                    existingOpen.id(),
                                    existingOpen.applicantId(),
                                    existingOpen.competencyId(),
                                    existingOpen.status(),
                                    qualification.trim(),
                                    experience,
                                    existingOpen.reviewerId(),
                                    existingOpen.decisionNote(),
                                    existingOpen.decidedAt(),
                                    existingOpen.createdAt(),
                                    now),
                            now);
            verifications.update(saved);
        } else {
            saved =
                    new Verification(
                            UUID.randomUUID(),
                            actorId,
                            competencyId,
                            VerificationStatus.SUBMITTED,
                            qualification.trim(),
                            experience,
                            null,
                            null,
                            null,
                            now,
                            now);
            verifications.save(saved);
        }
        if (evidence != null) {
            for (EvidenceInput item : evidence) {
                if (item == null || item.summary() == null || item.summary().isBlank()) {
                    continue;
                }
                verifications.saveEvidence(
                        new VerificationEvidence(
                                UUID.randomUUID(),
                                saved.id(),
                                item.kind() == null ? "other" : item.kind(),
                                item.summary(),
                                item.referenceUrl(),
                                item.storageKey(),
                                now));
            }
        }
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.VERIFICATION_SUBMITTED,
                        "Verification",
                        saved.id(),
                        correlationId,
                        Map.of("competencyId", competencyId.toString())));
        return saved;
    }

    public List<Verification> listMine(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        return verifications.listByApplicant(actorId);
    }
}
