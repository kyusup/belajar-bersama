package id.belajarbersama.domain.qa;

import id.belajarbersama.domain.identity.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QaAnswerRepository {
    void save(QaAnswer answer);

    void update(QaAnswer answer);

    Optional<QaAnswer> findById(UUID id);

    List<QaAnswer> listByQuestion(UUID questionId, boolean includeHidden);

    int usefulCount(UUID answerId);

    boolean markedUseful(UserId userId, UUID answerId);

    boolean addUseful(UserId userId, UUID answerId);

    void removeUseful(UserId userId, UUID answerId);
}
