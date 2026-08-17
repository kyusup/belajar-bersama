package id.belajarbersama.domain.search;

public record SearchQuery(String text, int page, int size) {
    public SearchQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
