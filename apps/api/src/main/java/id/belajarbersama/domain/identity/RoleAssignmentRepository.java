package id.belajarbersama.domain.identity;

import id.belajarbersama.domain.authorization.Role;
import java.time.Instant;
import java.util.Set;

public interface RoleAssignmentRepository {
    void assign(UserId userId, Role role, UserId assignedBy, Instant at);

    void revoke(UserId userId, Role role);

    Set<Role> rolesOf(UserId userId);

    boolean hasRole(UserId userId, Role role);
}
