package id.belajarbersama.domain.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.belajarbersama.domain.error.ValidationException;
import org.junit.jupiter.api.Test;

class UploadConstraintsTest {
    @Test
    void acceptsAllowedPdf() {
        assertDoesNotThrow(() -> UploadConstraints.validate("application/pdf", 1024));
    }

    @Test
    void rejectsExecutableMime() {
        assertThrows(
                ValidationException.class,
                () -> UploadConstraints.validate("application/x-msdownload", 1024));
    }

    @Test
    void rejectsOversizedFile() {
        assertThrows(
                ValidationException.class,
                () ->
                        UploadConstraints.validate(
                                "image/png", UploadConstraints.MAX_SIZE_BYTES + 1));
    }
}
