package id.belajarbersama.interfaces.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PlatformResourceTest {
    @Test
    void healthIsAnonymousAndUp() {
        given().when()
                .get("/api/v1/health")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", notNullValue())
                .body("status", equalTo("UP"))
                .body("service", equalTo("belajar-bersama-api"));
    }

    @Test
    void statusIncludesDatabase() {
        given().header("X-Correlation-Id", "test-correlation")
                .when()
                .get("/api/v1/status")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", equalTo("test-correlation"))
                .body("status", equalTo("UP"))
                .body("components.database.status", equalTo("UP"))
                .body("components.database.provider", equalTo("postgresql"))
                .body("components.search.provider", equalTo("postgres"))
                .body("components.storage.provider", equalTo("memory"));
    }
}
