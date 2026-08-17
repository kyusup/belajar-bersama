package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * End-to-end API journey across identity, content workflow, learning, Q&A, and moderation.
 * Complements browser E2E in {@code apps/web/e2e}.
 */
@QuarkusTest
class CriticalJourneyResourceTest {
    private static final String MATH = "aaaaaaaa-0001-4000-8000-000000000001";
    private static final String SUBJECT_MATH = "bbbbbbbb-0001-4000-8000-000000000001";
    private static final String LEVEL_SMP = "cccccccc-0002-4000-8000-000000000001";
    private static final String JOURNEY_TOKEN = "JourneyGoldenPath";

    @Test
    void mvpGoldenPathFromVerificationThroughModeration() {
        Cookie maker = verifiedContributor("journey-maker", MATH);
        Cookie checker = verifiedChecker("journey-checker", MATH);
        Cookie learner = login("GOOGLE", unique("journey-learner"), "Pelajar Perjalanan");
        Cookie moderator = moderator("journey-mod");

        String marker = JOURNEY_TOKEN + "-" + UUID.randomUUID();
        Published material = publishMaterial(maker, checker, marker);

        given().when()
                .get("/api/v1/public/content/" + material.slug)
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("public"))
                .body("currentRevision.title", containsString(marker))
                .body("status", equalTo("PUBLISHED"));

        given().when()
                .get("/api/v1/public/search?q=" + marker)
                .then()
                .statusCode(200)
                .body("items.title", hasItem(containsString(marker)));

        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("contentId", material.id))
                .post("/api/v1/me/bookmarks")
                .then()
                .statusCode(204);
        given().cookie(learner)
                .get("/api/v1/me/bookmarks")
                .then()
                .statusCode(200)
                .body("id", hasItem(material.id));

        String questionId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "title",
                                        marker + " QA",
                                        "body",
                                        "Bagaimana cara memahami " + marker + "?"))
                        .post("/api/v1/qa")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given().when()
                .get("/api/v1/public/qa/" + questionId)
                .then()
                .statusCode(200)
                .body("title", containsString(marker));

        Cookie answerer = login("GOOGLE", unique("journey-answerer"), "Penjawab Perjalanan");
        String answerId =
                given().cookie(answerer)
                        .contentType(ContentType.JSON)
                        .body(Map.of("body", "Baca materi " + marker + " terlebih dahulu."))
                        .post("/api/v1/qa/" + questionId + "/answers")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("answers[0].id");

        given().cookie(learner)
                .post("/api/v1/qa/" + questionId + "/accept/" + answerId)
                .then()
                .statusCode(200)
                .body("acceptedAnswerId", equalTo(answerId));

        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "reason",
                                "INCORRECT",
                                "description",
                                "Contoh pada " + marker + " perlu diperjelas."))
                .post("/api/v1/content/" + material.id + "/reports")
                .then()
                .statusCode(200)
                .body("status", equalTo("OPEN"));

        String reportId =
                given().cookie(moderator)
                        .get("/api/v1/moderation/content-reports")
                        .then()
                        .statusCode(200)
                        .body("contentId", hasItem(material.id))
                        .extract()
                        .path("find { it.contentId == '" + material.id + "' }.id");

        given().cookie(moderator)
                .post("/api/v1/moderation/content-reports/" + reportId + "/resolve")
                .then()
                .statusCode(200)
                .body("status", equalTo("RESOLVED"));

        given().cookie(moderator)
                .get("/api/v1/moderation/content-reports")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(reportId)));

        given().when()
                .get("/api/v1/public/content/" + material.slug)
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"));
    }

    private record Published(String id, String slug) {}

    private static Published publishMaterial(Cookie maker, Cookie checker, String marker) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("kind", "MATERIAL");
        draft.put("title", marker);
        draft.put("summary", "Ringkasan perjalanan uji");
        draft.put("subjectId", SUBJECT_MATH);
        draft.put("educationLevelId", LEVEL_SMP);
        draft.put("competencyIds", List.of(MATH));
        draft.put("license", "CC_BY_SA");
        draft.put(
                "body",
                Map.of("blocks", List.of(Map.of("type", "paragraph", "text", "Isi " + marker))));
        draft.put(
                "sources",
                List.of(
                        Map.of(
                                "title",
                                "Sumber uji",
                                "author",
                                "Penulis",
                                "publisher",
                                "Penerbit",
                                "url",
                                "https://example.test/journey")));

        String contentId =
                given().cookie(maker)
                        .contentType(ContentType.JSON)
                        .body(draft)
                        .when()
                        .post("/api/v1/content")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("DRAFT"))
                        .extract()
                        .path("id");
        String slug =
                given().cookie(maker)
                        .get("/api/v1/content/" + contentId)
                        .then()
                        .extract()
                        .path("slug");

        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));

        String submissionId =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .statusCode(200)
                        .body("contentId", hasItem(contentId))
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }.id");

        given().cookie(checker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(200);
        given().cookie(checker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "Layak terbit untuk perjalanan uji."))
                .post("/api/v1/reviews/" + submissionId + "/approve")
                .then()
                .statusCode(200)
                .body("decision", equalTo("APPROVE"));

        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/publish")
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("publishedRevisionId", notNullValue());

        given().when()
                .get("/api/v1/public/content/" + slug)
                .then()
                .statusCode(200)
                .body("reviews", hasSize(0));

        return new Published(contentId, slug);
    }

    private static Cookie verifiedContributor(String prefix, String competencyId) {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique(prefix), prefix);
        String verificationId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "competencyId",
                                        competencyId,
                                        "qualification",
                                        "uji perjalanan"))
                        .post("/api/v1/verifications")
                        .then()
                        .extract()
                        .path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "ok"))
                .post("/api/v1/admin/verifications/" + verificationId + "/approve")
                .then()
                .statusCode(200);
        return learner;
    }

    private static Cookie verifiedChecker(String prefix, String competencyId) {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie user = verifiedContributor(prefix, competencyId);
        String userId = given().cookie(user).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "CHECKER"))
                .post("/api/v1/admin/users/" + userId + "/roles")
                .then()
                .statusCode(204);
        return user;
    }

    private static Cookie moderator(String prefix) {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie user = login("GOOGLE", unique(prefix), prefix);
        String userId = given().cookie(user).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "MODERATOR"))
                .post("/api/v1/admin/users/" + userId + "/roles")
                .then()
                .statusCode(204);
        return user;
    }

    private static Cookie login(String provider, String subject, String name) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("provider", provider, "subject", subject, "displayName", name))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(200)
                .extract()
                .detailedCookie("bb_session");
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
