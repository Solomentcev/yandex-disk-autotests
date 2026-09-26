package ru.yandex.disk.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.yandex.disk.client.DiskApiClient;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты информации о Диске.
 */
@Feature("Диск")
class DiskTest {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    @Test
    @Tag("smoke")
    @Story("Получение информации о диске")
    @Description(
            "Проверка получения информации о диске "
                    + "авторизованного пользователя"
    )
    void shouldGetDiskInfo() {

        Allure.step(
                "Получить и проверить информацию о диске",
                () -> diskApiClient
                        .getDiskInfo()
                        .then()
                        .statusCode(200)
                        .body("user", notNullValue())
        );
    }
}