package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentRevision(
        UUID id,
        UUID contentId,
        int revisionNumber,
        String title,
        String summary,
        ContentBody body,
        LicenseCode license,
        String changeSummary,
        UserId createdBy,
        Instant createdAt,
        List<UUID> competencyIds,
        List<ContentSource> sources) {}
