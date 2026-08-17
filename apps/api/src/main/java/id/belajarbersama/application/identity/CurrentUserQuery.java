package id.belajarbersama.application.identity;

import id.belajarbersama.domain.authorization.Permission;
import id.belajarbersama.domain.authorization.Role;
import id.belajarbersama.domain.authorization.RolePermissionCatalog;
import id.belajarbersama.domain.identity.Identity;
import id.belajarbersama.domain.identity.IdentityRepository;
import id.belajarbersama.domain.identity.RoleAssignmentRepository;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserRepository;
import id.belajarbersama.domain.verification.VerificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CurrentUserQuery {
    private final UserRepository users;
    private final IdentityRepository identities;
    private final RoleAssignmentRepository roles;
    private final VerificationRepository verifications;

    public CurrentUserQuery(
            UserRepository users,
            IdentityRepository identities,
            RoleAssignmentRepository roles,
            VerificationRepository verifications) {
        this.users = users;
        this.identities = identities;
        this.roles = roles;
        this.verifications = verifications;
    }

    public CurrentUserView load(UserId userId) {
        User user = users.findById(userId).orElseThrow();
        Set<Role> stored = roles.rolesOf(userId);
        Set<UUID> approved = verifications.approvedCompetencyIds(userId);
        Set<Role> effective =
                EnumSet.copyOf(stored.isEmpty() ? EnumSet.noneOf(Role.class) : stored);
        if (!approved.isEmpty()) {
            effective.add(Role.VERIFIED_CONTRIBUTOR);
        }
        Set<Permission> permissions = RolePermissionCatalog.permissionsFor(effective);
        List<Identity> identityList = identities.listByUser(userId);
        return new CurrentUserView(user, identityList, stored, effective, permissions, approved);
    }

    public record CurrentUserView(
            User user,
            List<Identity> identities,
            Set<Role> storedRoles,
            Set<Role> effectiveRoles,
            Set<Permission> permissions,
            Set<UUID> approvedCompetencyIds) {}
}
