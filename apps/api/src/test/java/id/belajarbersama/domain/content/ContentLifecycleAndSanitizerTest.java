package id.belajarbersama.domain.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.belajarbersama.domain.error.BusinessRuleViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentLifecycleAndSanitizerTest {
    @Test
    void submitAndApprovePathIsAllowed() {
        assertTrue(ContentLifecycle.allowed(ContentStatus.DRAFT, ContentStatus.SUBMITTED));
        assertTrue(ContentLifecycle.allowed(ContentStatus.SUBMITTED, ContentStatus.IN_REVIEW));
        assertTrue(ContentLifecycle.allowed(ContentStatus.IN_REVIEW, ContentStatus.APPROVED));
        assertTrue(ContentLifecycle.allowed(ContentStatus.APPROVED, ContentStatus.PUBLISHED));
        assertFalse(ContentLifecycle.allowed(ContentStatus.DRAFT, ContentStatus.PUBLISHED));
        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        ContentLifecycle.assertTransition(
                                ContentStatus.DRAFT, ContentStatus.PUBLISHED));
    }

    @Test
    void publishedEditReturnsToDraft() {
        assertTrue(ContentLifecycle.allowed(ContentStatus.PUBLISHED, ContentStatus.DRAFT));
    }

    @Test
    void sanitizerStripsHtmlAndUnsafeLinks() {
        ContentBody dirty =
                new ContentBody(
                        List.of(
                                new ContentBlock(
                                        "paragraph",
                                        null,
                                        "<script>alert(1)</script>Halo",
                                        false,
                                        null,
                                        null,
                                        null),
                                new ContentBlock(
                                        "link",
                                        null,
                                        "klik",
                                        false,
                                        null,
                                        null,
                                        "javascript:alert(1)")));
        ContentBody clean = ContentSanitizer.sanitize(dirty);
        assertEquals("Halo", clean.blocks().get(0).text());
        assertTrue(clean.blocks().get(0).text().indexOf('<') < 0);
        assertEquals(null, clean.blocks().get(1).href());
        assertTrue(ContentSanitizer.hasSubstance(clean));
    }
}
