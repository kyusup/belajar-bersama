package id.belajarbersama.application.verification;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.error.NotFoundException;
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
public class VerificationReviewService {
    private final CurrentUserQuery currentUserQuery;
    private final VerificationRepository verifications;
    private final AuditRecorder auditRecorder;

    public VerificationReviewService(
            CurrentUserQuery currentUserQuery,
            VerificationRepository verifications,
            AuditRecorder auditRecorder) {
        this.currentUserQuery = currentUserQuery;
        this.verifications = verifications;
        this.auditRecorder = auditRecorder;
    }

    public List<Verification> listPending(UserId actorId) {
        requireReview(actorId);
        return verifications.listByStatus(VerificationStatus.SUBMITTED);
    }

    public VerificationDetail view(UserId actorId, UUID id) {
        requireReview(actorId);
        Verification verification = require(id);
        return new VerificationDetail(verification, verifications.listEvidence(id));
    }

    public Verification startReview(UserId actorId, UUID id, String correlationId) {
        requireReview(actorId);
        Verification updated = VerificationPolicy.startReview(require(id), actorId, Instant.now());
        verifications.update(updated);
        audit(actorId, updated, AuditAction.VERIFICATION_REVIEW_STARTED, correlationId);
        return updated;
    }

    public Verification approve(UserId actorId, UUID id, String note, String correlationId) {
        requireApprove(actorId);
        Verification updated =
                VerificationPolicy.approve(require(id), actorId, note, Instant.now());
        verifications.update(updated);
        audit(actorId, updated, AuditAction.VERIFICATION_APPROVED, correlationId);
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.USER_VERIFIED,
                        "User",
                        updated.applicantId().value(),
                        correlationId,
                        Map.of("competencyId", updated.competencyId().toString())));
        return updated;
    }

    public Verification reject(UserId actorId, UUID id, String note, String correlationId) {
        requireApprove(actorId);
        Verification updated = VerificationPolicy.reject(require(id), actorId, note, Instant.now());
        verifications.update(updated);
        audit(actorId, updated, AuditAction.VERIFICATION_REJECTED, correlationId);
        return updated;
    }

    public Verification requestChanges(UserId actorId, UUID id, String note, String correlationId) {
        requireApprove(actorId);
        Verification updated =
                VerificationPolicy.requestChanges(require(id), actorId, note, Instant.now());
        verifications.update(updated);
        audit(actorId, updated, AuditAction.VERIFICATION_CHANGES_REQUESTED, correlationId);
        return updated;
    }

    public Verification revoke(UserId actorId, UUID id, String note, String correlationId) {
        requireApprove(actorId);
        Verification updated = VerificationPolicy.revoke(require(id), actorId, note, Instant.now());
        verifications.update(updated);
        audit(actorId, updated, AuditAction.VERIFICATION_REVOKED, correlationId);
        return updated;
    }

    private void requireReview(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.VERIFICATION_REVIEW);
    }

    private void requireApprove(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.VERIFICATION_APPROVE);
    }

    private Verification require(UUID id) {
        return verifications
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Verification not found."));
    }

    private void audit(
            UserId actorId, Verification verification, AuditAction action, String correlationId) {
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        action,
                        "Verification",
                        verification.id(),
                        correlationId,
                        Map.of(
                                "applicantId",
                                verification.applicantId().value().toString(),
                                "competencyId",
                                verification.competencyId().toString(),
                                "status",
                                verification.status().name())));
    }

    public record VerificationDetail(
            Verification verification, List<VerificationEvidence> evidence) {}
}
