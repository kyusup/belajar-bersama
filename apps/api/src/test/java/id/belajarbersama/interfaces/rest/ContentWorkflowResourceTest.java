package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ContentWorkflowResourceTest {
    private static final String MATH = "aaaaaaaa-0001-4000-8000-000000000001";
    private static final String STATS = "aaaaaaaa-0001-4000-8000-000000000002";
    private static final String JAVA = "aaaaaaaa-0001-4000-8000-000000000003";
    private static final String SUBJECT_MATH = "bbbbbbbb-0001-4000-8000-000000000001";
    private static final String LEVEL_SMP = "cccccccc-0002-4000-8000-000000000001";

    @Test
    void anonymousCanBrowseTaxonomyButCannotCreate() {
        given().when()
                .get("/api/v1/subjects")
                .then()
                .statusCode(200)
                .body("slug", hasItem("matematika"));
        given().when()
                .get("/api/v1/education-levels")
                .then()
                .statusCode(200)
                .body("slug", hasItem("smp"));
        given().when().get("/api/v1/public/subjects").then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(draft("anon", MATH, "Paragraf"))
                .when()
                .post("/api/v1/content")
                .then()
                .statusCode(401);
    }

    @Test
    void learnerCannotCreateAndUnrelatedCompetencyIsRejected() {
        Cookie learner = login("GOOGLE", unique("learner"), "Learner");
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(draft("learner", MATH, "Paragraf"))
                .when()
                .post("/api/v1/content")
                .then()
                .statusCode(403)
                .body("code", equalTo("USER_NOT_VERIFIED_FOR_COMPETENCY"));

        Cookie maker = verifiedContributor("maker-math", MATH);
        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("java-only", JAVA, "Paragraf Java"))
                .when()
                .post("/api/v1/content")
                .then()
                .statusCode(403)
                .body("code", equalTo("USER_NOT_VERIFIED_FOR_COMPETENCY"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("multi", List.of(MATH, STATS), "Paragraf"))
                .when()
                .post("/api/v1/content")
                .then()
                .statusCode(403);
    }

    @Test
    void suspendedContributorCannotCreate() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie maker = verifiedContributor("suspended-maker", MATH);
        String makerId = given().cookie(maker).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .post("/api/v1/admin/users/" + makerId + "/suspend")
                .then()
                .statusCode(204);
        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("suspended", MATH, "Paragraf"))
                .when()
                .post("/api/v1/content")
                .then()
                .statusCode(401);
    }

    @Test
    void makerCheckerPublishArchiveAndPublicIntegrity() {
        Cookie maker = verifiedContributor("pipeline-maker", MATH);
        Cookie checker = verifiedChecker("pipeline-checker", MATH);
        Cookie javaChecker = verifiedChecker("pipeline-java", JAVA);
        String makerId = given().cookie(maker).get("/api/v1/me").then().extract().path("id");

        String contentId =
                given().cookie(maker)
                        .contentType(ContentType.JSON)
                        .body(draft("Persamaan Linear", MATH, "ax + b = 0"))
                        .when()
                        .post("/api/v1/content")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("DRAFT"))
                        .body("currentRevision.createdBy", equalTo(makerId))
                        .extract()
                        .path("id");
        String slug =
                given().cookie(maker)
                        .get("/api/v1/content/" + contentId)
                        .then()
                        .extract()
                        .path("slug");
        String revision1 =
                given().cookie(maker)
                        .get("/api/v1/content/" + contentId)
                        .then()
                        .extract()
                        .path("currentRevision.id");

        given().when().get("/api/v1/public/content/" + slug).then().statusCode(404);

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Persamaan Linear", MATH, "ax + b = 0 untuk SMP"))
                .when()
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("currentRevision.id", equalTo(revision1))
                .body("currentRevision.body.blocks[0].text", equalTo("ax + b = 0 untuk SMP"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Kosong", MATH, ""))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(200);
        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(422)
                .body("code", equalTo("CONTENT_INCOMPLETE"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Persamaan Linear", MATH, "ax + b = 0 untuk SMP"))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(200);
        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Edited after submit", MATH, "tidak boleh"))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(422)
                .body("code", equalTo("CONTENT_NOT_EDITABLE"));

        Cookie other = verifiedContributor("other-draft", MATH);
        given().cookie(other).get("/api/v1/content/" + contentId).then().statusCode(404);
        given().cookie(other)
                .contentType(ContentType.JSON)
                .body(draft("hack", MATH, "hack"))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(404);

        String submissionId =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .statusCode(200)
                        .body("contentId", hasItem(contentId))
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }.id");

        given().cookie(maker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(403);
        given().cookie(javaChecker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(403)
                .body("code", equalTo("USER_NOT_VERIFIED_FOR_COMPETENCY"));

        given().cookie(checker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(200)
                .body("revisionId", equalTo(revision1))
                .body("decision", nullValue());

        given().cookie(checker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "Perjelas contoh."))
                .post("/api/v1/reviews/" + submissionId + "/request-changes")
                .then()
                .statusCode(200)
                .body("decision", equalTo("REQUEST_CHANGES"));

        given().cookie(maker)
                .get("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CHANGES_REQUESTED"))
                .body("reviews.decision", hasItem("REQUEST_CHANGES"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Persamaan Linear", MATH, "Revisi dengan contoh x = 2"))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("currentRevisionNumber", equalTo(2))
                .body("currentRevision.id", not(equalTo(revision1)));

        String revision2 =
                given().cookie(maker)
                        .get("/api/v1/content/" + contentId)
                        .then()
                        .extract()
                        .path("currentRevision.id");

        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));

        String submission2 =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }.id");
        given().cookie(checker)
                .post("/api/v1/reviews/" + submission2 + "/start")
                .then()
                .statusCode(200);
        given().cookie(checker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "Memadai."))
                .post("/api/v1/reviews/" + submission2 + "/approve")
                .then()
                .statusCode(200)
                .body("decision", equalTo("APPROVE"))
                .body("revisionId", equalTo(revision2));

        given().cookie(maker)
                .get("/api/v1/content/" + contentId)
                .then()
                .body("status", equalTo("APPROVED"))
                .body("currentRevision.body.blocks[0].text", equalTo("Revisi dengan contoh x = 2"));

        given().when().get("/api/v1/public/content/" + slug).then().statusCode(404);

        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/publish")
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("publishedRevisionId", equalTo(revision2));

        given().when()
                .get("/api/v1/public/content/" + slug)
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("currentRevision.id", equalTo(revision2))
                .body("currentRevision.body.blocks[0].text", equalTo("Revisi dengan contoh x = 2"))
                .body("reviews", hasSize(0))
                .body("makerDisplayName", notNullValue());

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("Persamaan Linear", MATH, "Draf baru setelah terbit"))
                .patch("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("DRAFT"))
                .body("currentRevisionNumber", equalTo(3))
                .body("publishedRevisionId", equalTo(revision2));

        given().when()
                .get("/api/v1/public/content/" + slug)
                .then()
                .statusCode(200)
                .body("currentRevision.id", equalTo(revision2))
                .body("currentRevision.body.blocks[0].text", equalTo("Revisi dengan contoh x = 2"));

        given().cookie(other)
                .get("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("currentRevision.id", equalTo(revision2));

        Cookie reporter = login("GOOGLE", unique("reporter"), "Reporter");
        given().cookie(reporter)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "INCORRECT", "description", "Ada kesalahan rumus."))
                .post("/api/v1/content/" + contentId + "/reports")
                .then()
                .statusCode(200);
        given().cookie(reporter)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "INCORRECT", "description", "Laporan kedua."))
                .post("/api/v1/content/" + contentId + "/reports")
                .then()
                .statusCode(409);

        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/archive")
                .then()
                .statusCode(200);
        given().when().get("/api/v1/public/content/" + slug).then().statusCode(404);
        given().cookie(maker)
                .get("/api/v1/content/" + contentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ARCHIVED"))
                .body("reviews", hasSize(greaterThan(0)));
        given().cookie(maker)
                .get("/api/v1/my/content/" + contentId + "/revisions")
                .then()
                .statusCode(200)
                .body("id", hasSize(greaterThan(1)));
    }

    @Test
    void makerWithCheckerRoleCannotReviewOwnContent() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie maker = verifiedContributor("self-review", MATH);
        String makerId = given().cookie(maker).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "CHECKER"))
                .post("/api/v1/admin/users/" + makerId + "/roles")
                .then()
                .statusCode(204);
        String contentId =
                given().cookie(maker)
                        .contentType(ContentType.JSON)
                        .body(draft("Sendiri", MATH, "Isi materi"))
                        .post("/api/v1/content")
                        .then()
                        .extract()
                        .path("id");
        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(200);
        String submissionId =
                given().cookie(maker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }");
        org.junit.jupiter.api.Assertions.assertNull(submissionId);
        Cookie checker = verifiedChecker("self-review-checker", MATH);
        String queueId =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }.id");
        given().cookie(maker)
                .post("/api/v1/reviews/" + queueId + "/start")
                .then()
                .statusCode(422)
                .body("code", equalTo("MAKER_CANNOT_REVIEW_OWN_CONTENT"));
        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "saya setujui"))
                .post("/api/v1/reviews/" + queueId + "/approve")
                .then()
                .statusCode(422)
                .body("code", equalTo("MAKER_CANNOT_REVIEW_OWN_CONTENT"));
    }

    @Test
    void xssIsStrippedOnCreate() {
        Cookie maker = verifiedContributor("xss-maker", MATH);
        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(draft("XSS", MATH, "<script>alert(1)</script>Aman"))
                .post("/api/v1/content")
                .then()
                .statusCode(200)
                .body("currentRevision.body.blocks[0].text", equalTo("Aman"));
    }

    private static Cookie verifiedContributor(String prefix, String competencyId) {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique(prefix), prefix);
        String verificationId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(Map.of("competencyId", competencyId, "qualification", "uji"))
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

    private static Map<String, Object> draft(String title, String competencyId, String paragraph) {
        return draft(title, List.of(competencyId), paragraph);
    }

    private static Map<String, Object> draft(
            String title, List<String> competencyIds, String paragraph) {
        return Map.of(
                "kind",
                "MATERIAL",
                "title",
                title + " " + UUID.randomUUID(),
                "summary",
                "Ringkasan",
                "subjectId",
                SUBJECT_MATH,
                "educationLevelId",
                LEVEL_SMP,
                "competencyIds",
                competencyIds,
                "license",
                "CC_BY_SA",
                "body",
                Map.of("blocks", List.of(Map.of("type", "paragraph", "text", paragraph))),
                "sources",
                List.of(
                        Map.of(
                                "title",
                                "Buku sumber",
                                "author",
                                "Penulis",
                                "publisher",
                                "Penerbit",
                                "url",
                                "https://example.test/sumber")));
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
