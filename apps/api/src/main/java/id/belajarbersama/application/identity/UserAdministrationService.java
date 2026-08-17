package id.belajarbersama.application.identity;

import id.belajarbersama.domain.audit.AuditAction;
import id.belajarbersama.domain.audit.AuditEvent;
import id.belajarbersama.domain.audit.AuditRecorder;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.NotFoundException;
import id.belajarbersama.domain.identity.RoleAssignmentRepository;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.identity.UserStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class UserAdministrationService {
    private static final Set<Role> PRIVILEGED =
            Set.of(Role.CHECKER, Role.MODERATOR, Role.ADMINISTRATOR, Role.VERIFIED_CONTRIBUTOR);

    private final UserRepository users;
    private final RoleAssignmentRepository roles;
    private final CurrentUserQuery currentUserQuery;
    private final AuditRecorder auditRecorder;
    private final SessionService sessionService;

    public UserAdministrationService(
            UserRepository users,
            RoleAssignmentRepository roles,
            CurrentUserQuery currentUserQuery,
            AuditRecorder auditRecorder,
            SessionService sessionService) {
        this.users = users;
        this.roles = roles;
        this.currentUserQuery = currentUserQuery;
        this.auditRecorder = auditRecorder;
        this.sessionService = sessionService;
    }

    public AdminUserPage list(UserId actorId, String query, int page, int size) {
        CurrentUserQuery.CurrentUserView actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.USER_MANAGE);
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 50);
        List<AdminUser> items = new ArrayList<>();
        for (User user : users.search(query, pageNumber, pageSize)) {
            items.add(toAdminUser(user));
        }
        return new AdminUserPage(items, pageNumber, pageSize, users.countSearch(query));
    }

    public AdminUser view(UserId actorId, UserId targetId) {
        CurrentUserQuery.CurrentUserView actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.USER_MANAGE);
        User target =
                users.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("User not found."));
        return toAdminUser(target);
    }

    public void assignRole(UserId actorId, UserId targetId, Role role, String correlationId) {
        CurrentUserQuery.CurrentUserView actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.ROLE_MANAGE);
        if (PRIVILEGED.contains(role) && actorId.equals(targetId)) {
            throw new AuthorizationException(
                    ErrorCodes.CANNOT_ASSIGN_OWN_PRIVILEGED_ROLE,
                    "Users cannot assign privileged roles to themselves.");
        }
        if (role == Role.VERIFIED_CONTRIBUTOR) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN,
                    "VERIFIED_CONTRIBUTOR is derived from approved verification.");
        }
        if (role == Role.LEARNER) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "LEARNER is assigned automatically on first login.");
        }
        User target =
                users.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("User not found."));
        roles.assign(target.id(), role, actorId, Instant.now());
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.ROLE_ASSIGNED,
                        "User",
                        targetId.value(),
                        correlationId,
                        Map.of("role", role.name())));
    }

    public void revokeRole(UserId actorId, UserId targetId, Role role, String correlationId) {
        CurrentUserQuery.CurrentUserView actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.ROLE_MANAGE);
        if (role == Role.LEARNER || role == Role.VERIFIED_CONTRIBUTOR) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "This role cannot be revoked directly.");
        }
        roles.revoke(targetId, role);
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        AuditAction.ROLE_REVOKED,
                        "User",
                        targetId.value(),
                        correlationId,
                        Map.of("role", role.name())));
    }

    public void suspend(UserId actorId, UserId targetId, String correlationId) {
        changeStatus(
                actorId, targetId, UserStatus.SUSPENDED, AuditAction.USER_SUSPENDED, correlationId);
        sessionService.revokeAll(targetId);
    }

    public void reactivate(UserId actorId, UserId targetId, String correlationId) {
        changeStatus(
                actorId, targetId, UserStatus.ACTIVE, AuditAction.USER_REACTIVATED, correlationId);
    }

    public void deactivate(UserId actorId, UserId targetId, String correlationId) {
        changeStatus(
                actorId,
                targetId,
                UserStatus.DEACTIVATED,
                AuditAction.USER_DEACTIVATED,
                correlationId);
        sessionService.revokeAll(targetId);
    }

    private void changeStatus(
            UserId actorId,
            UserId targetId,
            UserStatus status,
            AuditAction action,
            String correlationId) {
        CurrentUserQuery.CurrentUserView actor = currentUserQuery.load(actorId);
        AuthorizationPolicies.assertActive(actor.user());
        AuthorizationPolicies.assertHasPermission(actor.permissions(), Permission.USER_MANAGE);
        User target =
                users.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("User not found."));
        users.update(target.withStatus(status, Instant.now()));
        auditRecorder.record(
                AuditEvent.of(
                        actorId,
                        action,
                        "User",
                        targetId.value(),
                        correlationId,
                        Map.of("status", status.name())));
    }

    private AdminUser toAdminUser(User user) {
        return new AdminUser(
                user.id(),
                user.displayName(),
                user.status(),
                user.createdAt(),
                roles.rolesOf(user.id()));
    }

    public record AdminUser(
            UserId id,
            String displayName,
            UserStatus status,
            Instant createdAt,
            Set<Role> storedRoles) {}

    public record AdminUserPage(List<AdminUser> items, int page, int size, long totalItems) {}
}
