package id.belajarbersama.domain.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentReportRepository {
    void save(ContentReport report);

    void update(ContentReport report);

    Optional<ContentReport> findById(UUID id);

    List<ContentReport> listByContent(UUID contentId);

    List<ContentReport> listOpen();

    boolean hasOpenReport(UUID reporterId, UUID contentId);
}
