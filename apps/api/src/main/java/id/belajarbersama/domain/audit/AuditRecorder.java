package id.belajarbersama.domain.audit;

public interface AuditRecorder {
    void record(AuditEvent event);
}
