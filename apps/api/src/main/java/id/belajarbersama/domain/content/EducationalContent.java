package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record EducationalContent(
        UUID id,
        ContentKind kind,
        String slug,
        UserId makerId,
        UUID subjectId,
        UUID educationLevelId,
        UUID parentId,
        ContentStatus status,
        UUID currentRevisionId,
        UUID publishedRevisionId,
        Instant archivedAt,
        int sortOrder,
        boolean required,
        int version,
        Instant createdAt,
        Instant updatedAt) {
    public boolean publiclyVisible() {
        return publishedRevisionId != null && archivedAt == null;
    }

    public boolean ownedBy(UserId userId) {
        return makerId.equals(userId);
    }

    public EducationalContent withWorkflow(
            ContentStatus nextStatus,
            UUID nextCurrentRevisionId,
            UUID nextPublishedRevisionId,
            Instant nextArchivedAt,
            Instant nextUpdatedAt) {
        return new EducationalContent(
                id,
                kind,
                slug,
                makerId,
                subjectId,
                educationLevelId,
                parentId,
                nextStatus,
                nextCurrentRevisionId,
                nextPublishedRevisionId,
                nextArchivedAt,
                sortOrder,
                required,
                version,
                createdAt,
                nextUpdatedAt);
    }
}
