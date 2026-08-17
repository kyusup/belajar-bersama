package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentSubmissionRepository {
    void save(ContentSubmission submission);

    boolean updateIfVersion(ContentSubmission submission, int expectedVersion);

    Optional<ContentSubmission> findById(UUID id);

    Optional<ContentSubmission> findOpenByContent(UUID contentId);

    List<ContentSubmission> listQueue();

    List<ContentSubmission> listAssignedTo(UserId checkerId);

    List<ContentSubmission> listByContent(UUID contentId);
}
