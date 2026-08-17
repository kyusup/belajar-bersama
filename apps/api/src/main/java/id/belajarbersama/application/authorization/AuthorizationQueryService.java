package id.belajarbersama.application.authorization;

import id.belajarbersama.application.identity.CurrentUserQuery;
import id.belajarbersama.domain.authorization.AuthorizationPolicies;
import id.belajarbersama.domain.identity.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class AuthorizationQueryService {
    private final CurrentUserQuery currentUserQuery;

    public AuthorizationQueryService(CurrentUserQuery currentUserQuery) {
        this.currentUserQuery = currentUserQuery;
    }

    public boolean canCreateContent(UserId userId, UUID competencyId) {
        var view = currentUserQuery.load(userId);
        return AuthorizationPolicies.canCreateContent(
                view.user(), view.permissions(), view.approvedCompetencyIds(), competencyId);
    }

    public boolean canReview(UserId userId, UUID competencyId, UserId makerId) {
        var view = currentUserQuery.load(userId);
        return AuthorizationPolicies.canReview(
                view.user(),
                view.storedRoles(),
                view.approvedCompetencyIds(),
                competencyId,
                makerId);
    }
}
