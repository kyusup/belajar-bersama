package id.belajarbersama.domain.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentReviewRepository {
    void save(ContentReview review);

    void update(ContentReview review);

    Optional<ContentReview> findById(UUID id);

    List<ContentReview> listBySubmission(UUID submissionId);

    List<ContentReview> listByContent(UUID contentId);
}
