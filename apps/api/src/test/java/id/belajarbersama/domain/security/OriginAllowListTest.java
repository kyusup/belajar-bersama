package id.belajarbersama.domain.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OriginAllowListTest {
    private static final Set<String> ALLOWED =
            OriginAllowList.parse("http://localhost:3000, https://belajar.example");

    @Test
    void missingOriginIsAllowedForNonBrowserClients() {
        assertTrue(OriginAllowList.allowed(null, ALLOWED));
        assertTrue(OriginAllowList.allowed("  ", ALLOWED));
    }

    @Test
    void listedOriginsAreAllowed() {
        assertTrue(OriginAllowList.allowed("http://localhost:3000", ALLOWED));
        assertTrue(OriginAllowList.allowed("https://belajar.example/path", ALLOWED));
        assertTrue(OriginAllowList.allowed("http://localhost:3000/", ALLOWED));
    }

    @Test
    void foreignOriginsAreRejected() {
        assertFalse(OriginAllowList.allowed("https://evil.example", ALLOWED));
        assertFalse(OriginAllowList.allowed("javascript:alert(1)", ALLOWED));
    }

    @Test
    void extractOriginStripsPathAndNormalizesHost() {
        assertEquals(
                "https://belajar.example",
                OriginAllowList.extractOrigin("https://BELAJAR.example/akun"));
        assertEquals(
                "http://localhost:3000",
                OriginAllowList.extractOrigin("http://localhost:3000/tanya"));
        assertEquals("", OriginAllowList.extractOrigin("ftp://files.example"));
    }
}
