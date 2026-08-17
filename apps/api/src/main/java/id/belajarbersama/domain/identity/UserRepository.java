package id.belajarbersama.domain.identity;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);

    void update(User user);

    Optional<User> findById(UserId id);

    List<User> search(String query, int page, int size);

    long countSearch(String query);
}
