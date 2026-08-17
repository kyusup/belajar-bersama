package id.belajarbersama.domain.storage;

import id.belajarbersama.domain.error.ErrorCodes;
import id.belajarbersama.domain.error.ValidationException;
import java.util.Set;

/** Upload constraints. Files are never treated as executable application resources. */
public final class UploadConstraints {
    public static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L;

    public static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private UploadConstraints() {}

    public static void validate(String contentType, long sizeBytes) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException(
                    "This file type is not allowed.",
                    java.util.Map.of("code", ErrorCodes.UPLOAD_NOT_ALLOWED));
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new ValidationException(
                    "File exceeds the allowed size.", java.util.Map.of("maxBytes", MAX_SIZE_BYTES));
        }
    }
}
