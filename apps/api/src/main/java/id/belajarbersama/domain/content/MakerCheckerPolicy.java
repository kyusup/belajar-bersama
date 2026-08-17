package id.belajarbersama.domain.content;

import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.ValidationException;
import id.belajarbersama.domain.identity.UserId;

/** Hard Maker–Checker rules. The backend rejects self-review even if the UI offers the action. */
public final class MakerCheckerPolicy {
    private MakerCheckerPolicy() {}

    public static void assertCheckerIsNotMaker(UserId maker, UserId checker) {
        if (maker == null || checker == null) {
            throw new ValidationException("Maker and checker are required.");
        }
        if (maker.equals(checker)) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.MAKER_CANNOT_REVIEW_OWN_CONTENT,
                    "A maker cannot review or approve their own submission.");
        }
    }
}
