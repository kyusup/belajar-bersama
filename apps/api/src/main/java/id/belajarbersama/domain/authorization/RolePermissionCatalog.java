package id.belajarbersama.domain.authorization;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Source of truth for Role → Permission. VERIFIED_CONTRIBUTOR is derived at runtime from approved
 * verification, not stored as a self-assigned role.
 */
public final class RolePermissionCatalog {
    private static final Map<Role, Set<Permission>> MATRIX = new EnumMap<>(Role.class);

    static {
        MATRIX.put(
                Role.LEARNER,
                EnumSet.of(
                        Permission.USER_READ_SELF,
                        Permission.CONTENT_READ_PUBLISHED,
                        Permission.LEARNING_PROGRESS_MANAGE,
                        Permission.BOOKMARK_MANAGE,
                        Permission.QUIZ_HISTORY_READ,
                        Permission.QA_CREATE,
                        Permission.QA_ASK,
                        Permission.QA_ANSWER,
                        Permission.QA_MARK_USEFUL,
                        Permission.CONTENT_REPORT,
                        Permission.VERIFICATION_APPLY));
        MATRIX.put(
                Role.VERIFIED_CONTRIBUTOR,
                EnumSet.of(
                        Permission.CONTENT_CREATE,
                        Permission.CONTENT_EDIT_OWN,
                        Permission.CONTENT_UPDATE_DRAFT,
                        Permission.CONTENT_SUBMIT,
                        Permission.CONTENT_PUBLISH,
                        Permission.CONTENT_ARCHIVE));
        MATRIX.put(
                Role.CHECKER,
                EnumSet.of(
                        Permission.CONTENT_REVIEW,
                        Permission.CONTENT_APPROVE,
                        Permission.CONTENT_REQUEST_CHANGES));
        MATRIX.put(
                Role.MODERATOR,
                EnumSet.of(Permission.CONTENT_MODERATE, Permission.CONTENT_REPORT_REVIEW));
        MATRIX.put(
                Role.ADMINISTRATOR,
                EnumSet.of(
                        Permission.VERIFICATION_REVIEW,
                        Permission.VERIFICATION_APPROVE,
                        Permission.VERIFICATION_REVOKE,
                        Permission.VERIFICATION_GRANT,
                        Permission.TAXONOMY_MANAGE,
                        Permission.USER_MANAGE,
                        Permission.ROLE_MANAGE,
                        Permission.ROLE_ASSIGN,
                        Permission.SYSTEM_ADMIN,
                        Permission.AUDIT_READ,
                        Permission.CONTENT_ARCHIVE,
                        Permission.CONTENT_PUBLISH));
    }

    private RolePermissionCatalog() {}

    public static Set<Permission> permissionsFor(Set<Role> roles) {
        EnumSet<Permission> granted = EnumSet.noneOf(Permission.class);
        if (roles == null) {
            return granted;
        }
        for (Role role : roles) {
            granted.addAll(MATRIX.getOrDefault(role, Set.of()));
        }
        return granted;
    }

    public static Set<Permission> permissionsFor(Role role) {
        return EnumSet.copyOf(MATRIX.getOrDefault(role, EnumSet.noneOf(Permission.class)));
    }
}
