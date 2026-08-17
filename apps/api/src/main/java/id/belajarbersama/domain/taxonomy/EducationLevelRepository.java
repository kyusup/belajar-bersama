package id.belajarbersama.domain.taxonomy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationLevelRepository {
    Optional<EducationLevel> findById(UUID id);

    List<EducationLevel> listActive();

    List<EducationLevel> listAll();

    void save(EducationLevel level);

    void update(EducationLevel level);
}
