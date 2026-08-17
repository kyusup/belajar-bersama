package id.belajarbersama.domain.error;

public final class ConflictException extends DomainException {
    public ConflictException(String code, String message) {
        super(code, message);
    }
}
