package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(RateLimitIntegrationResourceTest.LowLimitProfile.class)
class RateLimitIntegrationResourceTest {
    @Test
    void authBucketReturns429WhenExceeded() {
        Map<String, Object> body =
                Map.of("provider", "GOOGLE", "subject", "rate-limit-a", "displayName", "A");
        given().header("Origin", "http://localhost:3000")
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(200);
        given().header("Origin", "http://localhost:3000")
                .contentType(ContentType.JSON)
                .body(Map.of("provider", "GOOGLE", "subject", "rate-limit-b", "displayName", "B"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(200);
        given().header("Origin", "http://localhost:3000")
                .contentType(ContentType.JSON)
                .body(Map.of("provider", "GOOGLE", "subject", "rate-limit-c", "displayName", "C"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(429)
                .header("Retry-After", greaterThanOrEqualTo("1"))
                .body("code", equalTo("RATE_LIMITED"));
    }

    public static final class LowLimitProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "bb.rate-limit.enabled", "true",
                    "bb.rate-limit.auth-per-minute", "2");
        }
    }
}
