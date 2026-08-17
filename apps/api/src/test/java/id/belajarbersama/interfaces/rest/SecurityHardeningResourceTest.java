package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityHardeningResourceTest {
    @Test
    void foreignOriginCannotCallMutatingAuth() {
        given().header("Referer", "https://evil.example/login")
                .contentType(ContentType.JSON)
                .body(Map.of("provider", "GOOGLE", "subject", "csrf-evil", "displayName", "Evil"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(403)
                .body("code", equalTo("CSRF_ORIGIN_DENIED"));
    }

    @Test
    void allowedOriginCanCallMutatingAuth() {
        given().header("Origin", "http://localhost:3000")
                .contentType(ContentType.JSON)
                .body(Map.of("provider", "GOOGLE", "subject", "csrf-ok", "displayName", "Allowed"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(200);
    }

    @Test
    void healthIsNotRateLimitedAsAWrite() {
        given().when().get("/api/v1/health").then().statusCode(200);
    }
}
