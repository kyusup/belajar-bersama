package id.belajarbersama.domain.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;
import org.junit.jupiter.api.Test;

class MakerCheckerPolicyTest {
    @Test
    void rejectsSelfReview() {
        UserId same = UserId.newId();
        BusinessRuleViolationException exception =
                assertThrows(
                        BusinessRuleViolationException.class,
                        () -> MakerCheckerPolicy.assertCheckerIsNotMaker(same, same));
        assertEquals(ErrorCodes.MAKER_CANNOT_REVIEW_OWN_CONTENT, exception.code());
    }

    @Test
    void allowsDistinctChecker() {
        MakerCheckerPolicy.assertCheckerIsNotMaker(UserId.newId(), UserId.newId());
    }

    @Test
    void requiresBothActors() {
        assertThrows(
                ValidationException.class,
                () -> MakerCheckerPolicy.assertCheckerIsNotMaker(UserId.newId(), null));
    }
}
