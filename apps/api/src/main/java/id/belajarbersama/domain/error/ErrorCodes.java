package id.belajarbersama.domain.error;

public final class ErrorCodes {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INFRASTRUCTURE_FAILURE = "INFRASTRUCTURE_FAILURE";
    public static final String UNEXPECTED_FAILURE = "UNEXPECTED_FAILURE";

    public static final String MAKER_CANNOT_REVIEW_OWN_CONTENT = "MAKER_CANNOT_REVIEW_OWN_CONTENT";
    public static final String USER_NOT_VERIFIED_FOR_COMPETENCY =
            "USER_NOT_VERIFIED_FOR_COMPETENCY";
    public static final String CONTENT_NOT_SUBMITTED = "CONTENT_NOT_SUBMITTED";
    public static final String CONTENT_ALREADY_PUBLISHED = "CONTENT_ALREADY_PUBLISHED";
    public static final String INVALID_CONTENT_TRANSITION = "INVALID_CONTENT_TRANSITION";
    public static final String CONTENT_NOT_REVIEWABLE = "CONTENT_NOT_REVIEWABLE";
    public static final String UPLOAD_NOT_ALLOWED = "UPLOAD_NOT_ALLOWED";

    public static final String USER_NOT_ACTIVE = "USER_NOT_ACTIVE";
    public static final String CANNOT_VERIFY_SELF = "CANNOT_VERIFY_SELF";
    public static final String CANNOT_ASSIGN_OWN_PRIVILEGED_ROLE =
            "CANNOT_ASSIGN_OWN_PRIVILEGED_ROLE";
    public static final String INVALID_VERIFICATION_TRANSITION = "INVALID_VERIFICATION_TRANSITION";
    public static final String AUTH_PROVIDER_NOT_CONFIGURED = "AUTH_PROVIDER_NOT_CONFIGURED";
    public static final String AUTH_INVALID_IDENTITY = "AUTH_INVALID_IDENTITY";
    public static final String DEV_LOGIN_DISABLED = "DEV_LOGIN_DISABLED";
    public static final String CONTENT_NOT_EDITABLE = "CONTENT_NOT_EDITABLE";
    public static final String CONTENT_INCOMPLETE = "CONTENT_INCOMPLETE";
    public static final String SLUG_CONFLICT = "SLUG_CONFLICT";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    public static final String REVIEW_ALREADY_ACTIVE = "REVIEW_ALREADY_ACTIVE";
    public static final String QUIZ_NOT_PUBLISHED = "QUIZ_NOT_PUBLISHED";
    public static final String ATTEMPT_NOT_FOUND = "ATTEMPT_NOT_FOUND";
    public static final String ATTEMPT_ALREADY_SUBMITTED = "ATTEMPT_ALREADY_SUBMITTED";
    public static final String MAX_ATTEMPTS_REACHED = "MAX_ATTEMPTS_REACHED";
    public static final String INVALID_QUESTION_ANSWER = "INVALID_QUESTION_ANSWER";
    public static final String LESSON_NOT_PUBLISHED = "LESSON_NOT_PUBLISHED";
    public static final String CONTENT_NOT_AVAILABLE = "CONTENT_NOT_AVAILABLE";
    public static final String ATTEMPT_NOT_IN_PROGRESS = "ATTEMPT_NOT_IN_PROGRESS";
    public static final String QA_NOT_FOUND = "QA_NOT_FOUND";
    public static final String QA_CLOSED = "QA_CLOSED";
    public static final String QA_NOT_AUTHOR = "QA_NOT_AUTHOR";
    public static final String ANSWER_NOT_FOUND = "ANSWER_NOT_FOUND";
    public static final String CANNOT_MARK_OWN_ANSWER = "CANNOT_MARK_OWN_ANSWER";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String CSRF_ORIGIN_DENIED = "CSRF_ORIGIN_DENIED";

    private ErrorCodes() {}
}
