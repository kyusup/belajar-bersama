package id.belajarbersama.domain.identity;

import java.util.Optional;

public interface OauthStateRepository {
    void save(OauthState state);

    Optional<OauthState> consume(String state);
}
