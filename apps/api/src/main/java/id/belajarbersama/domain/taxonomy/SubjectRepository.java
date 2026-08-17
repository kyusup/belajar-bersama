package id.belajarbersama.domain.taxonomy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    Optional<Subject> findById(UUID id);

    Optional<Subject> findBySlug(String slug);

    List<Subject> listActive();

    List<Subject> listAll();

    void save(Subject subject);

    void update(Subject subject);
}
