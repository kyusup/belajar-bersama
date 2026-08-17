package id.belajarbersama.domain.learning;

import java.util.Optional;
import java.util.UUID;

public interface QuizSpecRepository {
    void replace(QuizSpec spec);

    Optional<QuizSpec> findByRevision(UUID revisionId);
}
