package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DevLoginDisabledResourceTest.DisabledProfile.class)
class DevLoginDisabledResourceTest {
    @Test
    void devLoginIsRejectedWhenDisabled() {
        given().header("Origin", "http://localhost:3000")
                .contentType(ContentType.JSON)
                .body(Map.of("provider", "GOOGLE", "subject", "blocked", "displayName", "Blocked"))
                .when()
                .post("/api/v1/auth/dev/login")
                .then()
                .statusCode(403)
                .body("code", equalTo("DEV_LOGIN_DISABLED"));
    }

    public static final class DisabledProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("bb.auth.dev-login.enabled", "false");
        }
    }
}
