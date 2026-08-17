package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LearningExperienceResourceTest {
    private static final String MATH = "aaaaaaaa-0001-4000-8000-000000000001";
    private static final String SUBJECT_MATH = "bbbbbbbb-0001-4000-8000-000000000001";
    private static final String LEVEL_SMP = "cccccccc-0002-4000-8000-000000000001";

    @Test
    void anonymousCanBrowsePublishedContentButNotDraftsOrPrivateData() {
        Cookie maker = verifiedContributor("learn-public-maker", MATH);
        Cookie checker = verifiedChecker("learn-public-checker", MATH);
        Published lesson = publish(maker, checker, lessonDraft("Peluang", null, 1));
        String draftId =
                given().cookie(maker)
                        .contentType(ContentType.JSON)
                        .body(lessonDraft("Draf rahasia", null, 2))
                        .post("/api/v1/content")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");
        String draftSlug =
                given().cookie(maker)
                        .get("/api/v1/content/" + draftId)
                        .then()
                        .extract()
                        .path("slug");

        given().when()
                .get("/api/v1/public/content/" + lesson.slug)
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("public"))
                .body("status", equalTo("PUBLISHED"))
                .body("reviews", hasSize(0));
        given().when().get("/api/v1/public/content/" + draftSlug).then().statusCode(404);
        given().when().get("/api/v1/me/progress/" + lesson.id).then().statusCode(401);
        given().when().get("/api/v1/me/bookmarks").then().statusCode(401);
        given().when().get("/api/v1/me/quiz-history").then().statusCode(401);
        given().when()
                .get("/api/v1/public/search?q=Peluang")
                .then()
                .statusCode(200)
                .body("items.title", hasItem(containsString("Peluang")));
    }

    @Test
    void lessonCompletionBookmarksAndProgressArePrivateAndIdempotent() {
        Cookie maker = verifiedContributor("learn-progress-maker", MATH);
        Cookie checker = verifiedChecker("learn-progress-checker", MATH);
        Cookie learner = login("GOOGLE", unique("learner-a"), "Learner A");
        Cookie other = login("GOOGLE", unique("learner-b"), "Learner B");
        Published course =
                publish(maker, checker, structuralDraft("COURSE", "Dasar Peluang", null, 0));
        Published lesson = publish(maker, checker, lessonDraft("Menghitung peluang", course.id, 1));

        given().when()
                .get("/api/v1/public/courses/" + course.slug)
                .then()
                .statusCode(200)
                .body("children.slug", hasItem(lesson.slug));

        given().cookie(learner)
                .post("/api/v1/me/lessons/" + lesson.id + "/complete")
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("private"))
                .body("completed", equalTo(1))
                .body("total", equalTo(1))
                .body("percent", equalTo(100))
                .body("lessonCompleted", equalTo(true));
        given().cookie(learner)
                .post("/api/v1/me/lessons/" + lesson.id + "/complete")
                .then()
                .statusCode(200)
                .body("percent", equalTo(100));
        given().cookie(other)
                .get("/api/v1/me/progress/" + course.id)
                .then()
                .statusCode(200)
                .body("percent", equalTo(0))
                .body("completed", equalTo(0));
        given().cookie(learner)
                .get("/api/v1/me/progress/" + course.id)
                .then()
                .statusCode(200)
                .body("percent", equalTo(100));

        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("contentId", lesson.id))
                .post("/api/v1/me/bookmarks")
                .then()
                .statusCode(204);
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("contentId", lesson.id))
                .post("/api/v1/me/bookmarks")
                .then()
                .statusCode(204);
        given().cookie(learner)
                .get("/api/v1/me/bookmarks")
                .then()
                .statusCode(200)
                .body("id", hasItem(lesson.id));
        given().cookie(other)
                .get("/api/v1/me/bookmarks")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(lesson.id)));
        given().cookie(other).delete("/api/v1/me/bookmarks/" + lesson.id).then().statusCode(204);
        given().cookie(learner)
                .get("/api/v1/me/bookmarks")
                .then()
                .statusCode(200)
                .body("id", hasItem(lesson.id));

        given().cookie(learner).post("/api/v1/me/opened/" + lesson.id).then().statusCode(204);
        given().cookie(learner)
                .get("/api/v1/me/continue")
                .then()
                .statusCode(200)
                .body("id", equalTo(lesson.id));
    }

    @Test
    void quizScoringIsServerSideSecureImmutableAndRevisionPinned() {
        Cookie maker = verifiedContributor("learn-quiz-maker", MATH);
        Cookie checker = verifiedChecker("learn-quiz-checker", MATH);
        Cookie learner = login("GOOGLE", unique("quiz-learner"), "Quiz Learner");
        Cookie other = login("GOOGLE", unique("quiz-other"), "Other Learner");
        Published quiz =
                publish(maker, checker, quizDraft("Kuis peluang", 70, 2, sampleQuestions()));

        given().when()
                .get("/api/v1/public/quizzes/" + quiz.slug)
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("public"))
                .body("questions[0].options[0]", not(hasKey("correct")))
                .body("questions[0]", not(hasKey("explanation")))
                .body(not(containsString("\"correct\"")));

        String q1 = publicQuestionId(quiz.slug, 0);
        String q2 = publicQuestionId(quiz.slug, 1);
        String q3 = publicQuestionId(quiz.slug, 2);
        String q1Correct = publicOptionId(quiz.slug, 0, 1);
        String q2Partial = publicOptionId(quiz.slug, 1, 0);
        String q2CorrectA = publicOptionId(quiz.slug, 1, 0);
        String q2CorrectB = publicOptionId(quiz.slug, 1, 2);
        String q3Correct = publicOptionId(quiz.slug, 2, 0);

        String attemptId =
                given().cookie(learner)
                        .post("/api/v1/me/quizzes/" + quiz.id + "/attempts")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("IN_PROGRESS"))
                        .body("scorePercent", nullValue())
                        .body("review", hasSize(0))
                        .extract()
                        .path("id");
        given().cookie(learner)
                .post("/api/v1/me/quizzes/" + quiz.id + "/attempts")
                .then()
                .statusCode(200)
                .body("id", equalTo(attemptId));

        given().cookie(other).get("/api/v1/me/attempts/" + attemptId).then().statusCode(404);
        given().cookie(other)
                .contentType(ContentType.JSON)
                .body(answers(q1, List.of(q1Correct)))
                .post("/api/v1/me/attempts/" + attemptId + "/submit")
                .then()
                .statusCode(404);

        Map<String, Object> manipulated = answers(q1, List.of(q1Correct));
        manipulated.put("score", 100);
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(manipulated)
                .post("/api/v1/me/attempts/" + attemptId + "/answers")
                .then()
                .statusCode(200)
                .body("scorePercent", nullValue());

        Map<String, Object> firstSubmit = answers(q1, List.of(q1Correct));
        mergeAnswer(firstSubmit, q2, List.of(q2Partial));
        mergeAnswer(firstSubmit, q3, List.of(q3Correct));
        firstSubmit.put("score", 100);
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(firstSubmit)
                .post("/api/v1/me/attempts/" + attemptId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"))
                .body("scorePercent", equalTo(66))
                .body("passed", equalTo(false))
                .body("correctCount", equalTo(2))
                .body("questionCount", equalTo(3))
                .body("review", hasSize(3))
                .body("review[0].correctOptionIds", notNullValue());

        String revision1 =
                given().cookie(learner)
                        .get("/api/v1/me/attempts/" + attemptId)
                        .then()
                        .statusCode(200)
                        .body("review[1].correct", equalTo(false))
                        .extract()
                        .path("quizRevisionId");

        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(answers(q1, List.of(q1Correct)))
                .post("/api/v1/me/attempts/" + attemptId + "/submit")
                .then()
                .statusCode(422)
                .body("code", equalTo("ATTEMPT_ALREADY_SUBMITTED"));
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(answers(q1, List.of(q1Correct)))
                .post("/api/v1/me/attempts/" + attemptId + "/answers")
                .then()
                .statusCode(422)
                .body("code", equalTo("ATTEMPT_ALREADY_SUBMITTED"));

        String attempt2 =
                given().cookie(learner)
                        .post("/api/v1/me/quizzes/" + quiz.id + "/attempts")
                        .then()
                        .statusCode(200)
                        .body("id", not(equalTo(attemptId)))
                        .body("status", equalTo("IN_PROGRESS"))
                        .extract()
                        .path("id");
        Map<String, Object> perfect = answers(q1, List.of(q1Correct));
        mergeAnswer(perfect, q2, List.of(q2CorrectA, q2CorrectB));
        mergeAnswer(perfect, q3, List.of(q3Correct));
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(perfect)
                .post("/api/v1/me/attempts/" + attempt2 + "/submit")
                .then()
                .statusCode(200)
                .body("scorePercent", equalTo(100))
                .body("passed", equalTo(true));

        given().cookie(learner)
                .post("/api/v1/me/quizzes/" + quiz.id + "/attempts")
                .then()
                .statusCode(422)
                .body("code", equalTo("MAX_ATTEMPTS_REACHED"));

        given().cookie(maker)
                .contentType(ContentType.JSON)
                .body(quizDraft("Kuis peluang v2", 70, 2, revisedQuestions()))
                .patch("/api/v1/content/" + quiz.id)
                .then()
                .statusCode(200);
        given().cookie(maker).post("/api/v1/content/" + quiz.id + "/submit").then().statusCode(200);
        String submissionId =
                given().cookie(checker)
                        .get("/api/v1/reviews/my")
                        .then()
                        .extract()
                        .path("find { it.contentId == '" + quiz.id + "' }.id");
        given().cookie(checker)
                .post("/api/v1/reviews/" + submissionId + "/start")
                .then()
                .statusCode(200);
        given().cookie(checker)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "Revisi kuis."))
                .post("/api/v1/reviews/" + submissionId + "/approve")
                .then()
                .statusCode(200);
        given().cookie(maker)
                .post("/api/v1/content/" + quiz.id + "/publish")
                .then()
                .statusCode(200);

        given().cookie(learner)
                .get("/api/v1/me/attempts/" + attemptId)
                .then()
                .statusCode(200)
                .body("quizRevisionId", equalTo(revision1))
                .body("scorePercent", equalTo(66));
        given().when()
                .get("/api/v1/public/quizzes/" + quiz.slug)
                .then()
                .statusCode(200)
                .body("questions[0].prompt", equalTo("Berapa 3 + 3?"));
    }

    private static String publicQuestionId(String slug, int index) {
        return given().get("/api/v1/public/quizzes/" + slug)
                .then()
                .extract()
                .path("questions[" + index + "].id");
    }

    private static String publicOptionId(String slug, int question, int option) {
        return given().get("/api/v1/public/quizzes/" + slug)
                .then()
                .extract()
                .path("questions[" + question + "].options[" + option + "].id");
    }

    private static Map<String, Object> answers(String questionId, List<String> optionIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> answers = new LinkedHashMap<>();
        answers.put(questionId, optionIds);
        body.put("answers", answers);
        return body;
    }

    @SuppressWarnings("unchecked")
    private static void mergeAnswer(
            Map<String, Object> body, String questionId, List<String> optionIds) {
        ((Map<String, Object>) body.get("answers")).put(questionId, optionIds);
    }

    private static List<Map<String, Object>> sampleQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        questions.add(
                question(
                        "SINGLE_CHOICE",
                        "Berapa 1 + 1?",
                        "Jumlahnya 2.",
                        List.of(
                                option("A", "1", false),
                                option("B", "2", true),
                                option("C", "3", false))));
        questions.add(
                question(
                        "MULTIPLE_CHOICE",
                        "Pilih bilangan genap.",
                        "2 dan 4 genap.",
                        List.of(
                                option("A", "2", true),
                                option("B", "3", false),
                                option("C", "4", true))));
        questions.add(
                question(
                        "TRUE_FALSE",
                        "Peluang selalu antara 0 dan 1.",
                        "Ya, peluang dinormalisasi ke [0,1].",
                        List.of(option("Benar", "Benar", true), option("Salah", "Salah", false))));
        return questions;
    }

    private static List<Map<String, Object>> revisedQuestions() {
        return List.of(
                question(
                        "SINGLE_CHOICE",
                        "Berapa 3 + 3?",
                        "Jumlahnya 6.",
                        List.of(option("A", "5", false), option("B", "6", true))));
    }

    private static Map<String, Object> question(
            String type, String prompt, String explanation, List<Map<String, Object>> options) {
        return map(
                "type",
                type,
                "prompt",
                prompt,
                "explanation",
                explanation,
                "difficulty",
                "EASY",
                "options",
                options);
    }

    private static Map<String, Object> option(String label, String text, boolean correct) {
        return map("label", label, "text", text, "correct", correct);
    }

    private static Published publish(Cookie maker, Cookie checker, Map<String, Object> draft) {
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
        String slug =
                given().cookie(maker)
                        .get("/api/v1/content/" + contentId)
                        .then()
                        .extract()
                        .path("slug");
        return new Published(contentId, slug);
    }

    private static Map<String, Object> lessonDraft(String title, String parentId, int sortOrder) {
        Map<String, Object> draft = structuralDraft("LESSON", title, parentId, sortOrder);
        draft.put(
                "body",
                Map.of(
                        "blocks",
                        List.of(Map.of("type", "paragraph", "text", "Isi pelajaran " + title))));
        return draft;
    }

    private static Map<String, Object> structuralDraft(
            String kind, String title, String parentId, int sortOrder) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("kind", kind);
        draft.put("title", title + " " + UUID.randomUUID());
        draft.put("summary", "Ringkasan " + title);
        draft.put("subjectId", SUBJECT_MATH);
        draft.put("educationLevelId", LEVEL_SMP);
        draft.put("competencyIds", List.of(MATH));
        draft.put("license", "CC_BY_SA");
        draft.put("sortOrder", sortOrder);
        draft.put("required", true);
        draft.put(
                "body",
                Map.of(
                        "blocks",
                        List.of(Map.of("type", "paragraph", "text", "Deskripsi " + title))));
        draft.put("sources", List.of());
        if (parentId != null) {
            draft.put("parentId", parentId);
        }
        return draft;
    }

    private static Map<String, Object> quizDraft(
            String title,
            Integer passingScore,
            Integer maxAttempts,
            List<Map<String, Object>> questions) {
        Map<String, Object> draft = structuralDraft("QUIZ", title, null, 0);
        draft.put("body", Map.of("blocks", List.of()));
        draft.put(
                "quiz",
                map(
                        "passingScore",
                        passingScore,
                        "maxAttempts",
                        maxAttempts,
                        "required",
                        true,
                        "questions",
                        questions));
        return draft;
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

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put((String) pairs[index], pairs[index + 1]);
        }
        return values;
    }

    private record Published(String id, String slug) {}
}
