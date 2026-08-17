package id.belajarbersama.domain.qa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QaQuestionRepository {
    void save(QaQuestion question);

    void update(QaQuestion question);

    Optional<QaQuestion> findById(UUID id);

    List<QaQuestion> listPublic(UUID contentId, UUID subjectId, int page, int size);

    long countPublic(UUID contentId, UUID subjectId);

    void index(QaQuestion question);

    void deleteIndex(UUID questionId);
}
