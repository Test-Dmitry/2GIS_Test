import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public class APITests {

    private static String token;

    // --- Получение токена перед всеми тестами ---
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://regions-test.2gis.com/v1";
        refreshToken();
        RestAssured.config = RestAssured.config()
                .encoderConfig(io.restassured.config.EncoderConfig.encoderConfig()
                        .defaultContentCharset("UTF-8"));
    }

    // --- Метод для обновления токена ---
    private static void refreshToken() {
        Response res = given()
                .when()
                .post("/auth/tokens")
                .then()
                .statusCode(200)
                .extract().response();
        token = res.getCookie("token");
        Assertions.assertNotNull(token, "Token не получен!");
    }

    // --- POST с raw body и автообновлением токена ---
    private Response postWithToken(String body, String contentType) {
        Response response = given()
                .cookie("token", token)
                .contentType(contentType)
                .body(body)
                .when()
                .post("/favorites");

        if (response.getStatusCode() == 401) {
            refreshToken();
            response = given()
                    .cookie("token", token)
                    .contentType(contentType)
                    .body(body)
                    .when()
                    .post("/favorites");
        }
        return response;
    }

    // --- POST с formParams и автообновлением токена ---
    private Response postWithTokenFormParams(String[][] params) {
        io.restassured.specification.RequestSpecification request = given()
                .cookie("token", token)
                .contentType("application/x-www-form-urlencoded");

        for (String[] param : params) {
            request.formParam(param[0], param[1]);
        }

        Response response = request
                .when()
                .post("/favorites");

        if (response.getStatusCode() == 401) {
            refreshToken();
            request = given()
                    .cookie("token", token)
                    .contentType("application/x-www-form-urlencoded");
            for (String[] param : params) {
                request.formParam(param[0], param[1]);
            }
            response = request
                    .when()
                    .post("/favorites");
        }

        return response;
    }

    // Smoke-тесты
    @Test
    @DisplayName("1. Smoke: Создание избранного без цвета")
    void createFavoriteWithoutColor() {
        String[][] params = {{"title","Централка"},{"lat","55.05"},{"lon","88.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("2. Smoke: Создание избранного с цветом RED")
    void createFavoriteWithColor() {
        String[][] params = {{"title","Централка"},{"lat","89.09"},{"lon","98.08"},{"color","RED"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("3. Smoke: Запрос без токена → 401 Unauthorized")
    void createFavoriteWithoutToken() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("title","Централка")
                .formParam("lat","89.09")
                .formParam("lon","98.08")
                .when()
                .post("/favorites")
                .then()
                .statusCode(401);
    }

    // Позитивные тесты
    @Test
    @DisplayName("4. Позитивный: Название с одним символом")
    void createFavoriteWithOneSymbol() {
        String[][] params = {{"title","F"},{"lat","20"},{"lon","50"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("5. Позитивный: Название с 999 символами")
    void createFavoriteWith999Symbol() {
        String longTitle = "F".repeat(999);
        String[][] params = {{"title", longTitle}, {"lat", "89.29682"}, {"lon", "98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("6. Позитивный: Название с цифрами")
    void createFavoriteWithOnlyNumbers() {
        String[][] params = {{"title","500"},{"lat","89.09"},{"lon","98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("7. Позитивный: Название со знаками препинания")
    void createFavoriteWithPunctuation() {
        String[][] params = {{"title","Централка, кофейня"},{"lat","89.09"},{"lon","98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("8. Позитивный: Широта 90")
    void createFavoriteWithLat90() {
        String[][] params = {{"title","Централка"},{"lat","90"},{"lon","98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("9. Позитивный: Широта -90")
    void createFavoriteWithLatMinus90() {
        String[][] params = {{"title","Централка"},{"lat","-90"},{"lon","98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("10. Позитивный: Широта 0")
    void createFavoriteWithLat0() {
        String[][] params = {{"title","Централка"},{"lat","0"},{"lon","98.08"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("11. Позитивный: Долгота -180")
    void createFavoriteWithLonMinus180() {
        String[][] params = {{"title","Централка"},{"lat","10"},{"lon","-180"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("12. Позитивный: Долгота 180")
    void createFavoriteWithLon180() {
        String[][] params = {{"title","Централка"},{"lat","10"},{"lon","180"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("13. Позитивный: Долгота 0")
    void createFavoriteWithLon0() {
        String[][] params = {{"title","Централка"},{"lat","10"},{"lon","0"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("14. Позитивный: Цвет Yellow")
    void createFavoriteWithYellow() {
        String[][] params = {{"title","Централка"},{"lat","20"},{"lon","70"},{"color","YELLOW"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("15. Позитивный: Цвет Blue")
    void createFavoriteWithBlue() {
        String[][] params = {{"title","Централка"},{"lat","20"},{"lon","70"},{"color","BLUE"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    @Test
    @DisplayName("16. Позитивный: Цвет Green")
    void createFavoriteWithGreen() {
        String[][] params = {{"title","Централка"},{"lat","20"},{"lon","70"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(200);
    }

    // Негативные
    @Test
    @DisplayName("17. Негативный: Отсутствует параметр title")
    void missingTitle() {
        String[][] params = {{"lat","20"},{"lon","180"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("18. Негативный: Отсутствует параметр lat")
    void missingLat() {
        String[][] params = {{"title","Централка"},{"lon","90"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("19. Негативный: Отсутствует параметр lon")
    void missingLon() {
        String[][] params = {{"title","Централка"},{"lat","50"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("20. Негативный: Параметр title пустой")
    void emptyTitle() {
        String[][] params = {{"title",""},{"lat","50"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("21. Негативный: В названии > 999 символов")
    void titleTooLong() {
        String longTitle = "F".repeat(1000);
        String[][] params = {{"title",longTitle},{"lat","50"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("22. Негативный: Параметр title из одного пробела")
    void titleOneSpace() {
        String[][] params = {{"title"," "},{"lat","50"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("23. Негативный: Параметр title из спецсимволов")
    void titleSpecialCharacters() {
        String[][] params = {{"title","!@&^"},{"lat","50"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("24. Негативный: Параметр lat строкой")
    void latString() {
        String[][] params = {{"title","Ff"},{"lat","string"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("24. Негативный: Широта -90.000001")
    void latBelowMinus90() {
        String[][] params = {{"title","Ff"},{"lat","-90.000001"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("25. Негативный: Широта 90.000001")
    void latAbove90() {
        String[][] params = {{"title","Ff"},{"lat","90.000001"},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("26. Негативный: Параметр lat пустой")
    void latEmpty() {
        String[][] params = {{"title","Ff"},{"lat",""},{"lon","40"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("27. Негативный: Параметр lon пустой")
    void lonEmpty() {
        String[][] params = {{"title","Ff"},{"lat","40"},{"lon",""},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("28. Негативный: Параметр lon строкой")
    void lonString() {
        String[][] params = {{"title","Ff"},{"lat","22"},{"lon","string"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("29. Негативный: Долгота -180.000001")
    void lonBelowMinus180() {
        String[][] params = {{"title","Ff"},{"lat","22"},{"lon","-180.000001"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("30. Негативный: Долгота 180.000001")
    void lonAbove180() {
        String[][] params = {{"title","Ff"},{"lat","11"},{"lon","180.000001"},{"color","GREEN"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("31. Негативный: Несуществующий цвет")
    void invalidColor() {
        String[][] params = {{"title","Ff"},{"lat","11"},{"lon","100"},{"color","BLACK"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("32. Негативный: Цвет в нижнем регистре")
    void colorLowerCase() {
        String[][] params = {{"title","Ff"},{"lat","11"},{"lon","100"},{"color","red"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("33. Негативный: Лишний параметр в запросе")
    void negativeExtraParam() {
        String[][] params = {{"title","Ff"},{"lat","11"},{"lon","22"},{"color","RED"},{"count","Russia"}};
        postWithTokenFormParams(params).then().statusCode(400);
    }

    @Test
    @DisplayName("34. Негативный: Замена Content-type на raw JSON")
    void negativeContentTypeRawJson() {
        String body = "{\n\"title\": \"Freeman’s\",\n\"lat\": 45.05,\n\"lon\": 78.08,\n\"color\": \"RED\"\n}";
        postWithToken(body,"application/json").then().statusCode(400);
    }
}