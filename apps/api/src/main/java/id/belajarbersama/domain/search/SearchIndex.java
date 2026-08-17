package id.belajarbersama.domain.search;

/**
 * Search port. PostgreSQL full-text search is the initial adapter. A dedicated engine may be
 * introduced later without changing callers.
 */
public interface SearchIndex {
    String provider();

    void index(SearchDocument document);

    void delete(String documentId);

    SearchPage search(SearchQuery query);

    boolean ping();
}
