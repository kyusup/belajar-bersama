package id.belajarbersama.domain.search;

public record SearchHit(String id, String type, String title, String slug, String summary) {
    public SearchHit(String id, String type, String title) {
        this(id, type, title, null, null);
    }
}
