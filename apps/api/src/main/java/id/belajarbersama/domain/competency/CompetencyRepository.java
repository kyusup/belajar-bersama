package id.belajarbersama.domain.competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetencyRepository {
    Optional<Competency> findById(UUID id);

    List<Competency> listActive();

    List<Competency> listAll();

    void save(Competency competency);

    void update(Competency competency);
}
