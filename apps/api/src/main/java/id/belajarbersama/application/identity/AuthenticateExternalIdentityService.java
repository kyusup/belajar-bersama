package id.belajarbersama.application.identity;

import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.identity.ExternalIdentityClaims;
import id.belajarbersama.domain.identity.Identity;
import id.belajarbersama.domain.identity.IdentityRepository;
import id.belajarbersama.domain.identity.RoleAssignmentRepository;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.identity.UserStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthenticateExternalIdentityService {
    private final IdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final AuditRecorder auditRecorder;
    private final List<String> bootstrapAdmins;

    public AuthenticateExternalIdentityService(
            IdentityRepository identityRepository,
            UserRepository userRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            AuditRecorder auditRecorder,
            @ConfigProperty(name = "bb.auth.bootstrap-admin-subjects", defaultValue = "")
                    String bootstrapAdmins) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.auditRecorder = auditRecorder;
        this.bootstrapAdmins =
                bootstrapAdmins.isBlank() || "unset".equalsIgnoreCase(bootstrapAdmins)
                        ? List.of()
                        : List.of(bootstrapAdmins.split("\\s*,\\s*"));
    }

    public User authenticate(ExternalIdentityClaims claims, String correlationId) {
        User user =
                identityRepository
                        .findByProviderSubject(claims.provider(), claims.issuer(), claims.subject())
                        .map(identity -> userRepository.findById(identity.userId()).orElseThrow())
                        .orElseGet(() -> provision(claims, correlationId));
        maybeGrantBootstrapAdmin(user, claims, correlationId);
        return user;
    }

    private User provision(ExternalIdentityClaims claims, String correlationId) {
        Instant now = Instant.now();
        UserId userId = UserId.newId();
        String displayName =
                claims.displayName() == null || claims.displayName().isBlank()
                        ? "Pengguna"
                        : claims.displayName().trim();
        User user = new User(userId, displayName, claims.avatarUrl(), UserStatus.ACTIVE, now, now);
        userRepository.save(user);
        Identity identity =
                new Identity(
                        UUID.randomUUID(),
                        userId,
                        claims.provider(),
                        claims.issuer(),
                        claims.subject(),
                        now);
        identityRepository.save(identity);
        roleAssignmentRepository.assign(userId, Role.LEARNER, null, now);
        auditRecorder.record(
                AuditEvent.of(
                        userId,
                        AuditAction.USER_CREATED,
                        "User",
                        userId.value(),
                        correlationId,
                        Map.of("provider", claims.provider().name())));
        auditRecorder.record(
                AuditEvent.of(
                        userId,
                        AuditAction.IDENTITY_LINKED,
                        "Identity",
                        identity.id(),
                        correlationId,
                        Map.of("provider", claims.provider().name())));
        return user;
    }

    private void maybeGrantBootstrapAdmin(
            User user, ExternalIdentityClaims claims, String correlationId) {
        String key = claims.provider().name() + ":" + claims.subject();
        if (!bootstrapAdmins.contains(key)
                || roleAssignmentRepository.hasRole(user.id(), Role.ADMINISTRATOR)) {
            return;
        }
        roleAssignmentRepository.assign(user.id(), Role.ADMINISTRATOR, null, Instant.now());
        auditRecorder.record(
                AuditEvent.of(
                        user.id(),
                        AuditAction.ROLE_ASSIGNED,
                        "User",
                        user.id().value(),
                        correlationId,
                        Map.of("role", Role.ADMINISTRATOR.name(), "bootstrap", true)));
    }
}
