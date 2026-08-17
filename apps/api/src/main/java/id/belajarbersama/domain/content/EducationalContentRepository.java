package id.belajarbersama.domain.content;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationalContentRepository {
    void save(EducationalContent content);

    boolean update(EducationalContent content);

    Optional<EducationalContent> findById(UUID id);

    Optional<EducationalContent> findBySlug(String slug);

    Optional<UUID> contentIdForSlugHistory(String slug);

    List<EducationalContent> listByMaker(UserId makerId);

    List<EducationalContent> listPublicBySubject(UUID subjectId);

    List<EducationalContent> listPublic();

    List<EducationalContent> listPublic(ContentKind kind, UUID subjectId);

    List<EducationalContent> listPublicChildren(UUID parentId);

    List<EducationalContent> listPublishedDescendants(UUID rootId);

    boolean slugTaken(String slug);

    void saveSlugHistory(String slug, UUID contentId);
}
