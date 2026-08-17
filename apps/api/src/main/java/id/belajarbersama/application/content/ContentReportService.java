package id.belajarbersama.application.content;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.content.ContentReport;
import id.belajarbersama.domain.content.ContentReportRepository;
import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.content.EducationalContentRepository;
import id.belajarbersama.domain.content.ReportReason;
import id.belajarbersama.domain.content.ReportStatus;
import id.belajarbersama.domain.error.ConflictException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ContentReportService {
    private final CurrentUserQuery currentUserQuery;
    private final EducationalContentRepository contents;
    private final ContentReportRepository reports;
    private final AuditRecorder auditRecorder;

    public ContentReportService(
            CurrentUserQuery currentUserQuery,
            EducationalContentRepository contents,
            ContentReportRepository reports,
            AuditRecorder auditRecorder) {
        this.currentUserQuery = currentUserQuery;
        this.contents = contents;
        this.reports = reports;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public ContentReport report(
            UserId actorId,
            UUID contentId,
            ReportReason reason,
            String description,
            String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.CONTENT_REPORT);
        var content =
                contents.findById(contentId)
                        .orElseThrow(() -> new NotFoundException("Content not found."));
        if (!content.publiclyVisible()) {
            throw new NotFoundException("Content not found.");
        }
        if (reason == null) {
            throw new ValidationException("Report reason is required.");
        }
        String text = ContentSanitizer.plainText(description);
        if (text.isBlank()) {
            throw new ValidationException("Report description is required.");
        }
        if (reports.hasOpenReport(actorId.value(), contentId)) {
            throw new ConflictException(
                    ErrorCodes.CONFLICT, "An open report already exists for this content.");
        }
        Instant now = Instant.now();
        ContentReport report =
                new ContentReport(
                        UUID.randomUUID(),
                        contentId,
                        actorId,
                        reason,
                        text,
                        ReportStatus.OPEN,
                        now,
                        now);
        reports.save(report);
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.CONTENT_REPORTED,
                        "ContentReport",
                        report.id(),
                        correlationId,
                        Map.of("contentId", contentId.toString(), "reason", reason.name())));
        return report;
    }

    public List<ContentReport> listOpen(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.CONTENT_REPORT_REVIEW);
        return reports.listOpen();
    }

    @Transactional
    public ContentReport resolve(
            UserId actorId, UUID reportId, ReportStatus status, String correlationId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(
                actor.permissions(), Permission.CONTENT_REPORT_REVIEW);
        if (status != ReportStatus.RESOLVED && status != ReportStatus.DISMISSED) {
            throw new ValidationException("Report can only be resolved or dismissed.");
        }
        ContentReport report =
                reports.findById(reportId)
                        .orElseThrow(() -> new NotFoundException("Report not found."));
        ContentReport updated =
                new ContentReport(
                        report.id(),
                        report.contentId(),
                        report.reporterId(),
                        report.reason(),
                        report.description(),
                        status,
                        report.createdAt(),
                        Instant.now());
        reports.update(updated);
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        status == ReportStatus.RESOLVED
                                ? AuditAction.CONTENT_REPORT_RESOLVED
                                : AuditAction.CONTENT_REPORT_DISMISSED,
                        "ContentReport",
                        report.id(),
                        correlationId,
                        Map.of("status", status.name())));
        return updated;
    }
}
