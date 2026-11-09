import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@Tag("ApiTests")
public class ApiTests {

    public static final String BASE_URI = "http://localhost:8080";
    public static final String LOGIN = "admin";
    public static final String PASSWORD = "secret123";

    RequestSpecification requestSpecification = new RequestSpecBuilder()
            .setBaseUri(BASE_URI)
            .setAuth(basic(LOGIN, PASSWORD))
            .build();

    public String uniqueName() {
        return "Test_" + UUID.randomUUID();
    }

    public float uniquePrice() {
        return Math.round(new Random().nextFloat(0, 100) * 100) / 100.0f;
    }

    public Integer createGood(String name, float price) {
        return given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %s
                        }
                        """.formatted(name, price))

                .when()
                .post("/goods/add")

                .then()
                .statusCode(200)
                .extract()
                .path("data.id");
    }

    // POST /goods/add — успешное создание товара
    @Test
    void addGoodSuccess() {
        String name = uniqueName();
        float price = uniquePrice();

        given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": %s
                        }
                        """.formatted(name, price))

                .when()
                .post("/goods/add")

                .then()
                .statusCode(200)
                .body("data.id", notNullValue())
                .body("message", equalTo("success"));
    }

    // POST /goods/add — некорректный JSON
    @Test
    void addGoodBadRequest() {
        given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body("{ invalid json }")

                .when()
                .post("/goods/add")

                .then()
                .statusCode(400);
    }

    // GET /goods/{id} — товар существует
    @Test
    void getGoodByIdSuccess() {
        Integer id = createGood(uniqueName(), uniquePrice());

        given()
                .spec(requestSpecification)

                .when()
                .get("/goods/{id}", id)

                .then()
                .statusCode(200);
    }

    // GET /goods/{id} — товар не существует
    @Test
    void getGoodByIdNotFound() {
        given()
                .spec(requestSpecification)

                .when()
                .get("/goods/{id}", Integer.MAX_VALUE)

                .then()
                .statusCode(404);
    }

    // DELETE /goods/{id} — успешное удаление
    @Test
    void deleteGoodSuccess() {
        Integer id = createGood(uniqueName(), uniquePrice());

        given()
                .spec(requestSpecification)

                .when()
                .delete("/goods/{id}", id)

                .then()
                .statusCode(200);
    }

    // DELETE /goods/{id} — товар не существует
    @Test
    void deleteGoodNotFound() {
        given()
                .spec(requestSpecification)

                .when()
                .delete("/goods/{id}", Integer.MAX_VALUE)

                .then()
                .statusCode(404);
    }

    // PATCH /goods/{id} — успешное обновление
    @Test
    void updateGoodSuccess() {
        Integer id = createGood(uniqueName(), uniquePrice());
        String newName = uniqueName();

        given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": 67.67
                        }
                        """.formatted(newName))

                .when()
                .patch("/goods/{id}", id)

                .then()
                .statusCode(200);

        given()
                .spec(requestSpecification)

                .when()
                .get("/goods/{id}", id)

                .then()
                .statusCode(200)
                .body("name", equalTo(newName));
    }

    // PATCH /goods/{id} — имя уже занято другим товаром
    @Test
    void updateGoodBadRequest() {
        String existingName = uniqueName();

        createGood(existingName, uniquePrice());
        Integer id = createGood(uniqueName(), uniquePrice());

        String body = """
            {
                "name": "%s",
                "price": %s
            }
            """.formatted(existingName, uniquePrice());

        given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body(body)

                .when()
                .patch("/goods/{id}", id)

                .then()
                .statusCode(400);
    }

    // PATCH /goods/{id} — товар не существует
    @Test
    void updateGoodNotFound() {
        given()
                .spec(requestSpecification)
                .contentType(JSON)
                .body("""
                        {
                            "name": "%s",
                            "price": 20.50
                        }
                        """.formatted(uniqueName()))

                .when()
                .patch("/goods/{id}", Integer.MAX_VALUE)

                .then()
                .statusCode(404);
    }

    // GET /goods/list — получение списка товаров
    @Test
    void getGoodsListSuccess() {
        given()
                .spec(requestSpecification)

                .when()
                .get("/goods/list")

                .then()
                .statusCode(200)
                .body("goods", notNullValue());
    }

    // GET /goods/list — получение списка с параметрами пагинации
    @Test
    void getGoodsListWithPagination() {
        given()
                .spec(requestSpecification)
                .queryParam("page", 0)
                .queryParam("size", 5)

                .when()
                .get("/goods/list")

                .then()
                .statusCode(200)
                .body("goods", notNullValue())
                .body("goods.size()", lessThanOrEqualTo(5));
    }

}
