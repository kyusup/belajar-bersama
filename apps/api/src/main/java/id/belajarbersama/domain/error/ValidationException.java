package id.belajarbersama.domain.error;

import java.util.Map;

public final class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(ErrorCodes.VALIDATION_FAILED, message);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(ErrorCodes.VALIDATION_FAILED, message, details);
    }
}
