package id.belajarbersama.infrastructure.search;

import id.belajarbersama.domain.search.SearchDocument;
import id.belajarbersama.domain.search.SearchHit;
import id.belajarbersama.domain.search.SearchIndex;
import id.belajarbersama.domain.search.SearchPage;
import id.belajarbersama.domain.search.SearchQuery;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

@ApplicationScoped
public class PostgresSearchIndex implements SearchIndex {
    private final DataSource dataSource;

    public PostgresSearchIndex(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String provider() {
        return "postgres";
    }

    @Override
    public void index(SearchDocument document) {
        Map<String, String> fields = document.fields() == null ? Map.of() : document.fields();
        String sql =
                """
                INSERT INTO content_search (content_id, title, summary, body_text, subject_name)
                VALUES (?::uuid, ?, ?, ?, ?)
                ON CONFLICT (content_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    body_text = EXCLUDED.body_text,
                    subject_name = EXCLUDED.subject_name
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, document.id());
            statement.setString(2, document.title());
            statement.setString(3, fields.getOrDefault("summary", ""));
            statement.setString(4, document.body() == null ? "" : document.body());
            statement.setString(5, fields.getOrDefault("subject", ""));
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new id.belajarbersama.domain.error.InfrastructureException(
                    "Failed to index content", exception);
        }
    }

    @Override
    public void delete(String documentId) {
        String sql = "DELETE FROM content_search WHERE content_id = ?::uuid";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentId);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new id.belajarbersama.domain.error.InfrastructureException(
                    "Failed to delete search document", exception);
        }
    }

    @Override
    public SearchPage search(SearchQuery query) {
        String text = query.text() == null ? "" : query.text().trim();
        if (text.isBlank()) {
            return new SearchPage(List.of(), query.page(), query.size(), 0);
        }
        String sql =
                """
                SELECT id, type, title, slug, summary FROM (
                    SELECT cs.content_id::text AS id, ec.kind AS type, cs.title, ec.slug, cs.summary,
                           ts_rank(cs.document, plainto_tsquery('simple', ?)) AS rank
                    FROM content_search cs
                    JOIN educational_content ec ON ec.id = cs.content_id
                    WHERE ec.published_revision_id IS NOT NULL
                      AND ec.archived_at IS NULL
                      AND cs.document @@ plainto_tsquery('simple', ?)
                    UNION ALL
                    SELECT q.id::text, 'QA_QUESTION', q.title, q.id::text,
                           left(q.body, 240),
                           ts_rank(qs.document, plainto_tsquery('simple', ?))
                    FROM qa_search qs
                    JOIN qa_question q ON q.id = qs.question_id
                    WHERE q.status <> 'HIDDEN'
                      AND qs.document @@ plainto_tsquery('simple', ?)
                ) hits
                ORDER BY rank DESC, title
                LIMIT ? OFFSET ?
                """;
        String countSql =
                """
                SELECT (
                    SELECT COUNT(*) FROM content_search cs
                    JOIN educational_content ec ON ec.id = cs.content_id
                    WHERE ec.published_revision_id IS NOT NULL
                      AND ec.archived_at IS NULL
                      AND cs.document @@ plainto_tsquery('simple', ?)
                ) + (
                    SELECT COUNT(*) FROM qa_search qs
                    JOIN qa_question q ON q.id = qs.question_id
                    WHERE q.status <> 'HIDDEN'
                      AND qs.document @@ plainto_tsquery('simple', ?)
                )
                """;
        try (Connection connection = dataSource.getConnection()) {
            long total = 0;
            try (PreparedStatement statement = connection.prepareStatement(countSql)) {
                statement.setString(1, text);
                statement.setString(2, text);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        total = resultSet.getLong(1);
                    }
                }
            }
            List<SearchHit> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, text);
                statement.setString(2, text);
                statement.setString(3, text);
                statement.setString(4, text);
                statement.setInt(5, query.size());
                statement.setInt(6, query.page() * query.size());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(
                                new SearchHit(
                                        resultSet.getString("id"),
                                        resultSet.getString("type"),
                                        resultSet.getString("title"),
                                        resultSet.getString("slug"),
                                        resultSet.getString("summary")));
                    }
                }
            }
            return new SearchPage(items, query.page(), query.size(), total);
        } catch (Exception exception) {
            throw new id.belajarbersama.domain.error.InfrastructureException(
                    "Failed to search content", exception);
        }
    }

    @Override
    public boolean ping() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception exception) {
            return false;
        }
    }
}
