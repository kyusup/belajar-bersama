package id.belajarbersama.domain.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.belajarbersama.domain.error.AuthorizationException;
import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.identity.User;
import id.belajarbersama.domain.identity.UserId;
import id.belajarbersama.domain.identity.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationPoliciesTest {
    private static final UUID MATH = UUID.fromString("aaaaaaaa-0001-4000-8000-000000000001");
    private static final UUID JAVA = UUID.fromString("aaaaaaaa-0001-4000-8000-000000000003");

    @Test
    void verifiedMathDoesNotGrantJava() {
        User user = activeUser();
        Set<Permission> permissions =
                RolePermissionCatalog.permissionsFor(
                        Set.of(Role.LEARNER, Role.VERIFIED_CONTRIBUTOR));
        assertTrue(AuthorizationPolicies.canCreateContent(user, permissions, Set.of(MATH), MATH));
        assertFalse(AuthorizationPolicies.canCreateContent(user, permissions, Set.of(MATH), JAVA));
    }

    @Test
    void unverifiedCannotCreate() {
        User user = activeUser();
        Set<Permission> learner = RolePermissionCatalog.permissionsFor(Role.LEARNER);
        assertFalse(AuthorizationPolicies.canCreateContent(user, learner, Set.of(), MATH));
        assertFalse(
                AuthorizationPolicies.canCreateContent(
                        user,
                        RolePermissionCatalog.permissionsFor(
                                Set.of(Role.LEARNER, Role.VERIFIED_CONTRIBUTOR)),
                        Set.of(),
                        MATH));
    }

    @Test
    void suspendedAndDeactivatedCannotCreate() {
        Set<Permission> permissions =
                RolePermissionCatalog.permissionsFor(
                        Set.of(Role.LEARNER, Role.VERIFIED_CONTRIBUTOR));
        for (UserStatus status : Set.of(UserStatus.SUSPENDED, UserStatus.DEACTIVATED)) {
            User user = userWith(status);
            assertFalse(
                    AuthorizationPolicies.canCreateContent(user, permissions, Set.of(MATH), MATH));
            assertThrows(
                    AuthorizationException.class,
                    () ->
                            AuthorizationPolicies.assertCanCreateContent(
                                    user, permissions, Set.of(MATH), MATH));
        }
    }

    @Test
    void checkerNeedsRoleAndCompetencyAndMustNotBeMaker() {
        User checker = activeUser();
        UserId maker = UserId.newId();
        assertFalse(AuthorizationPolicies.canReview(checker, Set.of(), Set.of(MATH), MATH, maker));
        assertTrue(
                AuthorizationPolicies.canReview(
                        checker, Set.of(Role.CHECKER), Set.of(MATH), MATH, maker));
        assertFalse(
                AuthorizationPolicies.canReview(
                        checker, Set.of(Role.CHECKER), Set.of(MATH), JAVA, maker));
        assertFalse(
                AuthorizationPolicies.canReview(
                        checker, Set.of(Role.CHECKER), Set.of(MATH), MATH, checker.id()));
        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        AuthorizationPolicies.assertCanReview(
                                checker, Set.of(Role.CHECKER), Set.of(MATH), MATH, checker.id()));
    }

    private static User activeUser() {
        return userWith(UserStatus.ACTIVE);
    }

    private static User userWith(UserStatus status) {
        Instant now = Instant.now();
        return new User(UserId.newId(), "Tester", null, status, now, now);
    }
}
