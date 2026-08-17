package id.belajarbersama.domain.content;

import id.belajarbersama.domain.error.BusinessRuleViolationException;
import id.belajarbersama.domain.error.ErrorCodes;
import java.util.Set;

public final class ContentLifecycle {
    private ContentLifecycle() {}

    public static void assertCanEdit(ContentStatus status) {
        if (status != ContentStatus.DRAFT && status != ContentStatus.CHANGES_REQUESTED) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.CONTENT_NOT_EDITABLE,
                    "Only draft or changes-requested content can be edited in place.");
        }
    }

    public static void assertTransition(ContentStatus from, ContentStatus to) {
        if (!allowed(from, to)) {
            throw new BusinessRuleViolationException(
                    ErrorCodes.INVALID_CONTENT_TRANSITION,
                    "Content cannot move from " + from + " to " + to + ".");
        }
    }

    public static boolean allowed(ContentStatus from, ContentStatus to) {
        return switch (from) {
            case DRAFT -> to == ContentStatus.SUBMITTED || to == ContentStatus.ARCHIVED;
            case SUBMITTED -> to == ContentStatus.IN_REVIEW || to == ContentStatus.ARCHIVED;
            case IN_REVIEW ->
                    to == ContentStatus.CHANGES_REQUESTED
                            || to == ContentStatus.APPROVED
                            || to == ContentStatus.ARCHIVED;
            case CHANGES_REQUESTED ->
                    to == ContentStatus.SUBMITTED
                            || to == ContentStatus.DRAFT
                            || to == ContentStatus.ARCHIVED;
            case APPROVED -> to == ContentStatus.PUBLISHED || to == ContentStatus.ARCHIVED;
            case PUBLISHED -> to == ContentStatus.DRAFT || to == ContentStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public static boolean isMutableRevision(ContentStatus status) {
        return status == ContentStatus.DRAFT || status == ContentStatus.CHANGES_REQUESTED;
    }

    public static Set<ContentStatus> workflowOpen() {
        return Set.of(
                ContentStatus.DRAFT,
                ContentStatus.SUBMITTED,
                ContentStatus.IN_REVIEW,
                ContentStatus.CHANGES_REQUESTED,
                ContentStatus.APPROVED);
    }
}
