package id.belajarbersama.domain.qa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QaReportRepository {
    void save(QaReport report);

    void update(QaReport report);

    Optional<QaReport> findById(UUID id);

    boolean hasOpenReport(UUID reporterId, QaTargetType type, UUID targetId);

    List<QaReport> listOpen();
}
