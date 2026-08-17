package id.belajarbersama.domain.error;

public final class AuthorizationException extends DomainException {
    public AuthorizationException(String code, String message) {
        super(code, message);
    }
}
