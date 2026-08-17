package id.belajarbersama.domain.learning;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.UUID;

public interface BookmarkRepository {
    void save(Bookmark bookmark);

    void delete(UserId userId, UUID contentId);

    boolean exists(UserId userId, UUID contentId);

    List<Bookmark> listByUser(UserId userId);
}
