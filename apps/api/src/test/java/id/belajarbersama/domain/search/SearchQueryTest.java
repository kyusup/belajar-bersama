package id.belajarbersama.domain.search;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SearchQueryTest {
    @Test
    void rejectsOversizedPage() {
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery("matematika", 0, 101));
    }
}
