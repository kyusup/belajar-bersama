package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IdentityAuthorizationResourceTest {
    private static final String MATH = "aaaaaaaa-0001-4000-8000-000000000001";
    private static final String JAVA = "aaaaaaaa-0001-4000-8000-000000000003";

    @Test
    void publicEndpointsRemainAnonymous() {
        given().when().get("/api/v1/health").then().statusCode(200);
        given().when().get("/api/v1/competencies").then().statusCode(200);
        given().when()
                .get("/api/v1/auth/config")
                .then()
                .statusCode(200)
                .body("devLogin", equalTo(true));
        given().when().get("/api/v1/me").then().statusCode(401);
        given().when().get("/api/v1/admin/verifications").then().statusCode(401);
    }

    @Test
    void firstLoginCreatesLearnerAndRepeatFindsSameUser() {
        String subject = unique("google-alice");
        Cookie session = login("GOOGLE", subject, "Alice");
        String userId =
                given().cookie(session)
                        .when()
                        .get("/api/v1/me")
                        .then()
                        .statusCode(200)
                        .body("roles", hasItem("LEARNER"))
                        .body("storedRoles", hasItem("LEARNER"))
                        .body("permissions", hasItem("VERIFICATION_APPLY"))
                        .body("permissions", not(hasItem("CONTENT_CREATE")))
                        .body("identities.provider", hasItem("GOOGLE"))
                        .extract()
                        .path("id");
        Cookie again = login("GOOGLE", subject, "Alice");
        given().cookie(again)
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(userId));
    }

    @Test
    void googleAndAppleMapToDistinctIdentities() {
        String subject = unique("multi");
        Cookie google = login("GOOGLE", subject, "Multi");
        String googleUser = given().cookie(google).get("/api/v1/me").then().extract().path("id");
        Cookie apple = login("APPLE", subject, "Multi");
        given().cookie(apple)
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("id", not(equalTo(googleUser)))
                .body("identities.provider", hasItem("APPLE"));
    }

    @Test
    void unknownProviderIsRejected() {
        given().contentType(ContentType.JSON)
                .body(Map.of("provider", "FACEBOOK", "subject", "x", "displayName", "X"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(400);
    }

    @Test
    void administratorDoesNotReceiveModeratorOrCheckerPermissionsByDefault() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        given().cookie(admin)
                .when()
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("roles", hasItem("ADMINISTRATOR"))
                .body("permissions", not(hasItem("CONTENT_MODERATE")))
                .body("permissions", not(hasItem("CONTENT_APPROVE")))
                .body("permissions", not(hasItem("CONTENT_REVIEW")));
    }

    @Test
    void userCannotAssignPrivilegedRoleToSelf() {
        Cookie session = login("GOOGLE", unique("self-role"), "Self");
        String userId = given().cookie(session).get("/api/v1/me").then().extract().path("id");
        given().cookie(session)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "ADMINISTRATOR"))
                .when()
                .post("/api/v1/admin/users/" + userId + "/roles")
                .then()
                .statusCode(403);
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        String adminId = given().cookie(admin).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "ADMINISTRATOR"))
                .when()
                .post("/api/v1/admin/users/" + adminId + "/roles")
                .then()
                .statusCode(403);
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "VERIFIED_CONTRIBUTOR"))
                .when()
                .post("/api/v1/admin/users/" + userId + "/roles")
                .then()
                .statusCode(403);
    }

    @Test
    void verificationWorkflowAndCompetencyScope() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique("maker"), "Maker");
        String learnerId = given().cookie(learner).get("/api/v1/me").then().extract().path("id");

        given().cookie(learner)
                .when()
                .get("/api/v1/authorization/can-create-content?competencyId=" + MATH)
                .then()
                .statusCode(200)
                .body("allowed", equalTo(false));
        given().cookie(learner).get("/api/v1/admin/verifications").then().statusCode(403);

        String verificationId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "competencyId",
                                        MATH,
                                        "qualification",
                                        "S1 Matematika",
                                        "experience",
                                        "5 tahun mengajar",
                                        "evidence",
                                        List.of(
                                                Map.of(
                                                        "kind",
                                                        "education",
                                                        "summary",
                                                        "Ijazah S1"))))
                        .when()
                        .post("/api/v1/verifications")
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("SUBMITTED"))
                        .extract()
                        .path("id");

        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "saya"))
                .when()
                .post("/api/v1/admin/verifications/" + verificationId + "/approve")
                .then()
                .statusCode(403);

        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "memadai"))
                .when()
                .post("/api/v1/admin/verifications/" + verificationId + "/approve")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("reviewerId", notNullValue());

        given().cookie(learner)
                .get("/api/v1/me")
                .then()
                .body("roles", hasItem("VERIFIED_CONTRIBUTOR"))
                .body("storedRoles", not(hasItem("VERIFIED_CONTRIBUTOR")))
                .body("storedRoles", hasItem("LEARNER"));

        given().cookie(learner)
                .get("/api/v1/authorization/can-create-content?competencyId=" + MATH)
                .then()
                .body("allowed", equalTo(true));
        given().cookie(learner)
                .get("/api/v1/authorization/can-create-content?competencyId=" + JAVA)
                .then()
                .body("allowed", equalTo(false));

        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("role", "CHECKER"))
                .post("/api/v1/admin/users/" + learnerId + "/roles")
                .then()
                .statusCode(204);

        given().cookie(learner)
                .get("/api/v1/me")
                .then()
                .body("roles", hasItem("CHECKER"))
                .body("roles", hasItem("LEARNER"));

        given().cookie(learner)
                .get(
                        "/api/v1/authorization/can-review?competencyId="
                                + MATH
                                + "&makerId="
                                + learnerId)
                .then()
                .body("allowed", equalTo(false));

        Cookie otherMaker = login("GOOGLE", unique("other-maker"), "Other");
        String otherId = given().cookie(otherMaker).get("/api/v1/me").then().extract().path("id");
        given().cookie(learner)
                .get(
                        "/api/v1/authorization/can-review?competencyId="
                                + MATH
                                + "&makerId="
                                + otherId)
                .then()
                .body("allowed", equalTo(true));
        given().cookie(learner)
                .get(
                        "/api/v1/authorization/can-review?competencyId="
                                + JAVA
                                + "&makerId="
                                + otherId)
                .then()
                .body("allowed", equalTo(false));

        Cookie notChecker = login("GOOGLE", unique("not-checker"), "NotChecker");
        given().cookie(notChecker)
                .get(
                        "/api/v1/authorization/can-review?competencyId="
                                + MATH
                                + "&makerId="
                                + otherId)
                .then()
                .body("allowed", equalTo(false));

        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "dicabut"))
                .post("/api/v1/admin/verifications/" + verificationId + "/revoke")
                .then()
                .statusCode(200)
                .body("status", equalTo("REVOKED"));

        given().cookie(learner)
                .get("/api/v1/authorization/can-create-content?competencyId=" + MATH)
                .then()
                .body("allowed", equalTo(false));
        given().cookie(learner)
                .get(
                        "/api/v1/authorization/can-review?competencyId="
                                + MATH
                                + "&makerId="
                                + otherId)
                .then()
                .body("allowed", equalTo(false));
    }

    @Test
    void rejectDoesNotGrantEligibilityAndSuspendedCannotAct() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique("rejected"), "Rejected");
        String learnerId = given().cookie(learner).get("/api/v1/me").then().extract().path("id");
        String verificationId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(Map.of("competencyId", MATH, "qualification", "kursus"))
                        .post("/api/v1/verifications")
                        .then()
                        .extract()
                        .path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "tidak cukup"))
                .post("/api/v1/admin/verifications/" + verificationId + "/reject")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"));
        given().cookie(learner)
                .get("/api/v1/authorization/can-create-content?competencyId=" + MATH)
                .then()
                .body("allowed", equalTo(false));

        given().cookie(admin)
                .post("/api/v1/admin/users/" + learnerId + "/suspend")
                .then()
                .statusCode(204);
        given().cookie(learner)
                .get("/api/v1/authorization/can-create-content?competencyId=" + MATH)
                .then()
                .statusCode(401);
        given().cookie(learner).get("/api/v1/me").then().statusCode(401);
    }

    @Test
    void deactivatedUserCannotUseSession() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique("deactivated"), "Deactivated");
        String learnerId = given().cookie(learner).get("/api/v1/me").then().extract().path("id");
        given().cookie(admin)
                .post("/api/v1/admin/users/" + learnerId + "/deactivate")
                .then()
                .statusCode(204);
        given().cookie(learner).get("/api/v1/me").then().statusCode(401);
    }

    @Test
    void requestChangesThenResubmit() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique("changes"), "Changes");
        String verificationId =
                given().cookie(learner)
                        .contentType(ContentType.JSON)
                        .body(Map.of("competencyId", MATH, "qualification", "awal"))
                        .post("/api/v1/verifications")
                        .then()
                        .extract()
                        .path("id");
        given().cookie(admin)
                .contentType(ContentType.JSON)
                .body(Map.of("note", "lengkapi"))
                .post("/api/v1/admin/verifications/" + verificationId + "/request-changes")
                .then()
                .statusCode(200)
                .body("status", equalTo("CHANGES_REQUESTED"));
        given().cookie(learner)
                .contentType(ContentType.JSON)
                .body(Map.of("competencyId", MATH, "qualification", "dilengkapi"))
                .post("/api/v1/verifications")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));
        given().cookie(learner)
                .get("/api/v1/verifications/me")
                .then()
                .statusCode(200)
                .body("status", hasItem("SUBMITTED"));
    }

    @Test
    void adminCanListUsersByDisplayNameWithoutEmailOrIdentities() {
        Cookie admin = login("GOOGLE", "admin-1", "Admin");
        Cookie learner = login("GOOGLE", unique("listed-learner"), "Sari Listed");
        String learnerId = given().cookie(learner).get("/api/v1/me").then().extract().path("id");
        given().cookie(learner).get("/api/v1/admin/users").then().statusCode(403);
        given().cookie(admin)
                .get("/api/v1/admin/users?q=Sari Listed")
                .then()
                .statusCode(200)
                .body("items.id", hasItem(learnerId))
                .body("items.displayName", hasItem("Sari Listed"))
                .body(
                        "items.find { it.id == '" + learnerId + "' }.storedRoles",
                        hasItem("LEARNER"));
        given().cookie(admin)
                .get("/api/v1/admin/users/" + learnerId)
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Sari Listed"))
                .body("id", equalTo(learnerId));
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
