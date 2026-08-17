package id.belajarbersama.domain.error;

import java.util.Map;

public abstract class DomainException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    protected DomainException(String code, String message) {
        this(code, message, Map.of());
    }

    protected DomainException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
