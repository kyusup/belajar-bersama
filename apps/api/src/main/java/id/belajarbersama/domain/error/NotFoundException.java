package id.belajarbersama.domain.error;

public final class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(ErrorCodes.RESOURCE_NOT_FOUND, message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
