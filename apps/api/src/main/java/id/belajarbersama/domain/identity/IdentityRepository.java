package id.belajarbersama.domain.identity;

import java.util.List;
import java.util.Optional;

public interface IdentityRepository {
    void save(Identity identity);

    Optional<Identity> findByProviderSubject(
            IdentityProviderId provider, String issuer, String subject);

    List<Identity> listByUser(UserId userId);
}
