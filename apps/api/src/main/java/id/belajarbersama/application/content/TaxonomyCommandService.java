package id.belajarbersama.application.content;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.competency.Competency;
import id.belajarbersama.domain.competency.CompetencyRepository;
import id.belajarbersama.domain.content.ContentSanitizer;
import id.belajarbersama.domain.content.Slugs;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.taxonomy.EducationLevel;
import id.belajarbersama.domain.taxonomy.EducationLevelRepository;
import id.belajarbersama.domain.taxonomy.Subject;
import id.belajarbersama.domain.taxonomy.SubjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class TaxonomyCommandService {
    private final CurrentUserQuery currentUserQuery;
    private final SubjectRepository subjects;
    private final EducationLevelRepository levels;
    private final CompetencyRepository competencies;

    public TaxonomyCommandService(
            CurrentUserQuery currentUserQuery,
            SubjectRepository subjects,
            EducationLevelRepository levels,
            CompetencyRepository competencies) {
        this.currentUserQuery = currentUserQuery;
        this.subjects = subjects;
        this.levels = levels;
        this.competencies = competencies;
    }

    @Transactional
    public Subject createSubject(UserId actorId, String name, String description) {
        requireTaxonomy(actorId);
        Instant now = Instant.now();
        String slug = uniqueSubjectSlug(Slugs.fromTitle(name));
        Subject subject =
                new Subject(
                        UUID.randomUUID(),
                        slug,
                        ContentSanitizer.plainText(name),
                        ContentSanitizer.plainText(description),
                        true,
                        now,
                        now);
        if (subject.name().isBlank()) {
            throw new ValidationException("Subject name is required.");
        }
        subjects.save(subject);
        return subject;
    }

    @Transactional
    public EducationLevel createLevel(UserId actorId, String name, int sortOrder) {
        requireTaxonomy(actorId);
        Instant now = Instant.now();
        EducationLevel level =
                new EducationLevel(
                        UUID.randomUUID(),
                        uniqueLevelSlug(Slugs.fromTitle(name)),
                        ContentSanitizer.plainText(name),
                        sortOrder,
                        true,
                        now,
                        now);
        if (level.name().isBlank()) {
            throw new ValidationException("Education level name is required.");
        }
        levels.save(level);
        return level;
    }

    @Transactional
    public Competency createCompetency(UserId actorId, String name, String description) {
        requireTaxonomy(actorId);
        Instant now = Instant.now();
        Competency competency =
                new Competency(
                        UUID.randomUUID(),
                        uniqueCompetencySlug(Slugs.fromTitle(name)),
                        ContentSanitizer.plainText(name),
                        ContentSanitizer.plainText(description),
                        true,
                        now,
                        now);
        if (competency.name().isBlank()) {
            throw new ValidationException("Competency name is required.");
        }
        competencies.save(competency);
        return competency;
    }

    private void requireTaxonomy(UserId actorId) {
        var actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.TAXONOMY_MANAGE);
    }

    private String uniqueSubjectSlug(String base) {
        String slug = base;
        int n = 0;
        while (subjects.findBySlug(slug).isPresent()) {
            n++;
            slug = base + "-" + n;
        }
        return slug;
    }

    private String uniqueLevelSlug(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String uniqueCompetencySlug(String base) {
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
