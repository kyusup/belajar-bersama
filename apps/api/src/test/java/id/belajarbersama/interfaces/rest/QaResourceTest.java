package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class QaResourceTest {
    private static final String MATH = "aaaaaaaa-0001-4000-8000-000000000001";
    private static final String SUBJECT_MATH = "bbbbbbbb-0001-4000-8000-000000000001";
    private static final String LEVEL_SMP = "cccccccc-0002-4000-8000-000000000001";

    @Test
    void anonymousCanReadPublicQaButNotWriteOrSeeHidden() {
        Cookie asker = login("GOOGLE", unique("qa-ask"), "Penanya");
        Cookie answerer = login("GOOGLE", unique("qa-ans"), "Penjawab");
        String token = "PeluangBayesQA-" + UUID.randomUUID();
        String questionId =
                given().cookie(asker)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "title",
                                        token,
                                        "body",
                                        "Mengapa peluang tidak boleh negatif?"))
                        .post("/api/v1/qa")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        given().when()
                .get("/api/v1/public/qa/" + questionId)
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("public"))
                .body("title", equalTo(token))
                .body("authorDisplayName", equalTo("Penanya"))
                .body("answers", hasSize(0));
        given().when()
                .get("/api/v1/public/qa")
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("public"));
        given().when()
                .get("/api/v1/public/search?q=" + token)
                .then()
                .statusCode(200)
                .body("items.title", hasItem(token))
                .body("items.type", hasItem("QA_QUESTION"));

        given().when()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Tidak boleh", "body", "Anonim tidak boleh bertanya."))
                .post("/api/v1/qa")
                .then()
                .statusCode(401);

        given().cookie(answerer)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Karena peluang dinormalisasi ke [0,1]."))
                .post("/api/v1/qa/" + questionId + "/answers")
                .then()
                .statusCode(200)
                .body("answers.body", hasItem("Karena peluang dinormalisasi ke [0,1]."));

        Cookie moderator = moderator("qa-mod-hide");
        given().cookie(moderator)
                .post("/api/v1/moderation/qa/" + questionId + "/hide")
                .then()
                .statusCode(204);
        given().when().get("/api/v1/public/qa/" + questionId).then().statusCode(404);
        given().when()
                .get("/api/v1/public/search?q=" + token)
                .then()
                .statusCode(200)
                .body("items.title", not(hasItem(token)));
        given().cookie(moderator)
                .get("/api/v1/moderation/qa/" + questionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("HIDDEN"));
    }

    @Test
    void askerAndModeratorCanAcceptButOtherLearnersCannot() {
        Cookie asker = login("GOOGLE", unique("qa-acc-ask"), "Asker Acc");
        Cookie other = login("GOOGLE", unique("qa-acc-oth"), "Other Acc");
        Cookie answerer = login("GOOGLE", unique("qa-acc-ans"), "Answer Acc");
        String questionId = ask(asker, "Apa itu peluang bersyarat?", "Mohon contoh SMP.");
        String answerId =
                given().cookie(answerer)
                        .contentType(ContentType.JSON)
                        .body(Map.of("body", "Peluang A jika B sudah terjadi."))
                        .post("/api/v1/qa/" + questionId + "/answers")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("answers[0].id");

        given().cookie(other)
                .post("/api/v1/qa/" + questionId + "/accept/" + answerId)
                .then()
                .statusCode(422)
                .body("code", equalTo("QA_NOT_AUTHOR"));
        given().cookie(asker)
                .post("/api/v1/qa/" + questionId + "/accept/" + answerId)
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("private"))
                .body("acceptedAnswerId", equalTo(answerId));
        given().cookie(asker)
                .delete("/api/v1/qa/" + questionId + "/accept")
                .then()
                .statusCode(200)
                .body("acceptedAnswerId", nullValue());

        Cookie moderator = moderator("qa-acc-mod");
        given().cookie(moderator)
                .post("/api/v1/qa/" + questionId + "/accept/" + answerId)
                .then()
                .statusCode(200)
                .body("acceptedAnswerId", equalTo(answerId));
    }

    @Test
    void usefulMarkCloseReportAndIdorRules() {
        Cookie asker = login("GOOGLE", unique("qa-rule-ask"), "Rule Asker");
        Cookie other = login("GOOGLE", unique("qa-rule-oth"), "Rule Other");
        Cookie answerer = login("GOOGLE", unique("qa-rule-ans"), "Rule Answerer");
        String questionId = ask(asker, "Bagaimana menghitung peluang?", "Butuh langkah.");
        String answerId =
                given().cookie(answerer)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "body",
                                        "Bagi kejadian menguntungkan dengan seluruh kejadian."))
                        .post("/api/v1/qa/" + questionId + "/answers")
                        .then()
                        .extract()
                        .path("answers[0].id");

        given().cookie(other)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Diretas", "body", "Bukan milik saya."))
                .patch("/api/v1/qa/" + questionId)
                .then()
                .statusCode(404);
        given().cookie(asker)
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "title",
                                "<script>alert(1)</script>XSS peluang " + UUID.randomUUID(),
                                "body",
                                "<b>teks</b> aman"))
                .patch("/api/v1/qa/" + questionId)
                .then()
                .statusCode(200)
                .body("title", not(containsString("<script")))
                .body("body", not(containsString("<b>")));

        given().cookie(answerer)
                .post("/api/v1/qa/answers/" + answerId + "/useful")
                .then()
                .statusCode(422)
                .body("code", equalTo("CANNOT_MARK_OWN_ANSWER"));
        given().cookie(other)
                .post("/api/v1/qa/answers/" + answerId + "/useful")
                .then()
                .statusCode(204);
        given().cookie(other)
                .get("/api/v1/public/qa/" + questionId)
                .then()
                .statusCode(200)
                .body("answers[0].usefulCount", equalTo(1))
                .body("answers[0].markedUseful", equalTo(true));

        given().cookie(asker)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "SPAM", "description", "Iklan tersembunyi."))
                .post("/api/v1/qa/" + questionId + "/reports")
                .then()
                .statusCode(200)
                .body("status", equalTo("OPEN"));
        given().cookie(asker)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "SPAM", "description", "Laporan kedua."))
                .post("/api/v1/qa/" + questionId + "/reports")
                .then()
                .statusCode(409);

        given().cookie(asker)
                .post("/api/v1/qa/" + questionId + "/close")
                .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
        given().cookie(other)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Terlambat menjawab."))
                .post("/api/v1/qa/" + questionId + "/answers")
                .then()
                .statusCode(422)
                .body("code", equalTo("QA_CLOSED"));

        Cookie moderator = moderator("qa-rule-mod");
        given().cookie(moderator)
                .get("/api/v1/moderation/reports")
                .then()
                .statusCode(200)
                .body("targetId", hasItem(questionId));
        String reportId =
                given().cookie(moderator)
                        .get("/api/v1/moderation/reports")
                        .then()
                        .extract()
                        .path("find { it.targetId == '" + questionId + "' }.id");
        given().cookie(moderator)
                .post("/api/v1/moderation/reports/" + reportId + "/resolve")
                .then()
                .statusCode(200)
                .body("status", equalTo("RESOLVED"));
        given().cookie(other).get("/api/v1/moderation/reports").then().statusCode(403);
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        given().cookie(admin)
                .post("/api/v1/moderation/qa/" + questionId + "/hide")
                .then()
                .statusCode(403);
    }

    @Test
    void contentReportsAppearOnModeratorQueue() {
        Cookie maker = verifiedContributor("qa-report-maker", MATH);
        Cookie checker = verifiedChecker("qa-report-checker", MATH);
        Cookie learner = login("GOOGLE", unique("qa-report-learner"), "Reporter");
        String contentId = publishLesson(maker, checker, "Peluang laporan");
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", "INCORRECT", "description", "Ada rumus yang keliru."))
                .post("/api/v1/content/" + contentId + "/reports")
                .then()
                .statusCode(200);
        Cookie moderator = moderator("qa-content-mod");
        given().cookie(moderator)
                .get("/api/v1/moderation/content-reports")
                .then()
                .statusCode(200)
                .body("contentId", hasItem(contentId))
                .body("find { it.contentId == '" + contentId + "' }.reporterId", notNullValue());
    }

    private static String ask(Cookie asker, String title, String body) {
        return given().cookie(asker)
                .contentType(ContentType.JSON)
                .body(Map.of("title", title + " " + UUID.randomUUID(), "body", body))
                .post("/api/v1/qa")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
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

    private static String publishLesson(Cookie maker, Cookie checker, String title) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("kind", "LESSON");
        draft.put("title", title + " " + UUID.randomUUID());
        draft.put("summary", "Ringkasan " + title);
        draft.put("subjectId", SUBJECT_MATH);
        draft.put("educationLevelId", LEVEL_SMP);
        draft.put("competencyIds", List.of(MATH));
        draft.put("license", "CC_BY_SA");
        draft.put("sortOrder", 1);
        draft.put("required", true);
        draft.put(
                "body",
                Map.of("blocks", List.of(Map.of("type", "paragraph", "text", "Isi " + title))));
        draft.put("sources", List.of());
        String contentId =
                given().cookie(maker)
                        .contentType(ContentType.JSON)
                        .body(draft)
                        .post("/api/v1/content")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");
        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/submit")
                .then()
                .statusCode(200);
        String submissionId =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .extract()
                        .path("find { it.contentId == '" + contentId + "' }.id");
        given().cookie(checker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(200);
        given().cookie(checker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "Layak terbit."))
                .post("/api/v1/reviews/" + submissionId + "/approve")
                .then()
                .statusCode(200);
        given().cookie(maker)
                .post("/api/v1/content/" + contentId + "/publish")
                .then()
                .statusCode(200);
        return contentId;
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
