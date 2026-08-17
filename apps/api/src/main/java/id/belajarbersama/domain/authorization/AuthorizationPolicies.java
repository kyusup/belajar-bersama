package id.belajarbersama.domain.authorization;

import id.belajarbersama.domain.content.MakerCheckerPolicy;
import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import java.util.Set;
import java.util.UUID;

/**
 * Domain authorization policies for contributor/checker eligibility. Content workflow services call
 * these methods on every write; query endpoints exist for UI gating only.
 */
public final class AuthorizationPolicies {
    private AuthorizationPolicies() {}

    public static boolean canCreateContent(
            User user,
            Set<Permission> permissions,
            Set<UUID> approvedCompetencyIds,
            UUID competencyId) {
        return user != null
                && user.isActive()
                && permissions != null
                && permissions.contains(Permission.CONTENT_CREATE)
                && competencyId != null
                && approvedCompetencyIds != null
                && approvedCompetencyIds.contains(competencyId);
    }

    public static void assertCanCreateContent(
            User user,
            Set<Permission> permissions,
            Set<UUID> approvedCompetencyIds,
            UUID competencyId) {
        assertActive(user);
        if (!canCreateContent(user, permissions, approvedCompetencyIds, competencyId)) {
            throw new AuthorizationException(
                    ErrorCodes.USER_NOT_VERIFIED_FOR_COMPETENCY,
                    "User is not verified for the requested competency.");
        }
    }

    public static boolean canReview(
            User user,
            Set<Role> storedRoles,
            Set<UUID> approvedCompetencyIds,
            UUID competencyId,
            UserId makerId) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (storedRoles == null || !storedRoles.contains(Role.CHECKER)) {
            return false;
        }
        if (competencyId == null
                || approvedCompetencyIds == null
                || !approvedCompetencyIds.contains(competencyId)) {
            return false;
        }
        if (makerId != null && makerId.equals(user.id())) {
            return false;
        }
        return true;
    }

    public static void assertCanReview(
            User user,
            Set<Role> storedRoles,
            Set<UUID> approvedCompetencyIds,
            UUID competencyId,
            UserId makerId) {
        assertActive(user);
        if (storedRoles == null || !storedRoles.contains(Role.CHECKER)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Checker permission is required to review content.");
        }
        if (approvedCompetencyIds == null || !approvedCompetencyIds.contains(competencyId)) {
            throw new AuthorizationException(
                    ErrorCodes.USER_NOT_VERIFIED_FOR_COMPETENCY,
                    "Checker is not eligible for the relevant competency.");
        }
        MakerCheckerPolicy.assertCheckerIsNotMaker(makerId, user.id());
    }

    public static boolean canCreateContent(
            User user,
            Set<Permission> permissions,
            Set<UUID> approvedCompetencyIds,
            Set<UUID> requiredCompetencyIds) {
        if (requiredCompetencyIds == null || requiredCompetencyIds.isEmpty()) {
            return false;
        }
        for (UUID competencyId : requiredCompetencyIds) {
            if (!canCreateContent(user, permissions, approvedCompetencyIds, competencyId)) {
                return false;
            }
        }
        return true;
    }

    public static void assertCanCreateContent(
            User user,
            Set<Permission> permissions,
            Set<UUID> approvedCompetencyIds,
            Set<UUID> requiredCompetencyIds) {
        assertActive(user);
        if (!canCreateContent(user, permissions, approvedCompetencyIds, requiredCompetencyIds)) {
            throw new AuthorizationException(
                    ErrorCodes.USER_NOT_VERIFIED_FOR_COMPETENCY,
                    "User is not verified for every required competency.");
        }
    }

    public static boolean canReview(
            User user,
            Set<Role> storedRoles,
            Set<UUID> approvedCompetencyIds,
            Set<UUID> requiredCompetencyIds,
            UserId makerId) {
        if (requiredCompetencyIds == null || requiredCompetencyIds.isEmpty()) {
            return false;
        }
        for (UUID competencyId : requiredCompetencyIds) {
            if (!canReview(user, storedRoles, approvedCompetencyIds, competencyId, makerId)) {
                return false;
            }
        }
        return true;
    }

    public static void assertCanReview(
            User user,
            Set<Role> storedRoles,
            Set<UUID> approvedCompetencyIds,
            Set<UUID> requiredCompetencyIds,
            UserId makerId) {
        assertActive(user);
        if (storedRoles == null || !storedRoles.contains(Role.CHECKER)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Checker permission is required to review content.");
        }
        if (requiredCompetencyIds == null || requiredCompetencyIds.isEmpty()) {
            throw new AuthorizationException(
                    ErrorCodes.USER_NOT_VERIFIED_FOR_COMPETENCY,
                    "Checker is not eligible for the relevant competency.");
        }
        for (UUID competencyId : requiredCompetencyIds) {
            if (approvedCompetencyIds == null || !approvedCompetencyIds.contains(competencyId)) {
                throw new AuthorizationException(
                        ErrorCodes.USER_NOT_VERIFIED_FOR_COMPETENCY,
                        "Checker is not eligible for the relevant competency.");
            }
        }
        MakerCheckerPolicy.assertCheckerIsNotMaker(makerId, user.id());
    }

    public static void assertActive(User user) {
        if (user == null || !user.isActive()) {
            throw new AuthorizationException(
                    ErrorCodes.USER_NOT_ACTIVE,
                    "A suspended or deactivated user cannot perform protected actions.");
        }
    }

    public static void assertHasPermission(Set<Permission> granted, Permission required) {
        if (granted == null || !granted.contains(required)) {
            throw new AuthorizationException(
                    ErrorCodes.FORBIDDEN, "Missing permission: " + required.name());
        }
    }
}
