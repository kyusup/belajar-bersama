package id.belajarbersama.domain.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRevisionRepository {
    void save(ContentRevision revision);

    void updateMutable(ContentRevision revision);

    Optional<ContentRevision> findById(UUID id);

    List<ContentRevision> listByContent(UUID contentId);

    int nextRevisionNumber(UUID contentId);
}
