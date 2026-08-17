package id.belajarbersama.domain.verification;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface VerificationRepository {
    void save(Verification verification);

    void update(Verification verification);

    Optional<Verification> findById(UUID id);

    List<Verification> listByApplicant(UserId applicantId);

    List<Verification> listByStatus(VerificationStatus status);

    Optional<Verification> findApproved(UserId userId, UUID competencyId);

    Set<UUID> approvedCompetencyIds(UserId userId);

    boolean hasOpenApplication(UserId userId, UUID competencyId);

    void saveEvidence(VerificationEvidence evidence);

    List<VerificationEvidence> listEvidence(UUID verificationId);
}
