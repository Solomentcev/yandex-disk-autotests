package ru.yandex.disk.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.data.TestData;
import ru.yandex.disk.extensions.TestDataExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты работы с Корзиной.
 */
@Feature("Корзина")
@ExtendWith(TestDataExtension.class)
class TrashTest {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    @Test
    @Tag("regression")
    @Story("Перемещение ресурса в Корзину и окончательное удаление")
    @Description(
            "Проверка перемещения ресурса в Корзину, получения ресурса "
                    + "из Корзины и окончательного удаления"
    )
    void shouldMoveResourceToTrashAndDeletePermanently() {
        String path = TestData.currentTestPath();

        Allure.step(
                "Проверить наличие каталога перед перемещением в Корзину: "
                        + path,
                () -> diskApiClient
                        .getResource(path)
                        .then()
                        .statusCode(200)
                        .body("path", equalTo("disk:" + path))
                        .body("type", equalTo("dir"))
        );

        Allure.step(
                "Переместить каталог в Корзину: " + path,
                () -> {
                    var response = diskApiClient.deleteResource(path);

                    int statusCode = response.getStatusCode();

                    if (statusCode == 202) {
                        String operationHref = response
                                .jsonPath()
                                .getString("href");

                        waitForOperation(
                                operationHref,
                                "Дождаться перемещения каталога "
                                        + "в Корзину: " + path
                        );
                    } else {
                        response.then().statusCode(204);
                    }
                }
        );

        Allure.step(
                "Проверить отсутствие каталога на Диске: " + path,
                () -> diskApiClient
                        .getResource(path)
                        .then()
                        .statusCode(404)
        );

        String trashPath = findResourceInTrash(path);

        Allure.step(
                "Получить ресурс из Корзины: " + trashPath,
                () -> diskApiClient
                        .getTrashResource(trashPath)
                        .then()
                        .statusCode(200)
                        .body(
                                "origin_path",
                                equalTo("disk:" + path)
                        )
        );

        Allure.step(
                "Окончательно удалить ресурс из Корзины: " + trashPath,
                () -> {
                    var response =
                            diskApiClient.deleteTrashResource(trashPath);

                    int statusCode = response.getStatusCode();

                    if (statusCode == 202) {
                        String operationHref = response
                                .jsonPath()
                                .getString("href");

                        waitForOperation(
                                operationHref,
                                "Дождаться окончательного удаления: "
                                        + trashPath
                        );
                    } else {
                        response.then().statusCode(204);
                    }
                }
        );

        Allure.step(
                "Проверить отсутствие ресурса в Корзине: " + trashPath,
                () -> diskApiClient
                        .getTrashResource(trashPath)
                        .then()
                        .statusCode(404)
        );
    }

    private String findResourceInTrash(String path) {
        String originPath = "disk:" + path;

        return Allure.step(
                "Найти ресурс в Корзине по исходному пути: "
                        + originPath,
                () -> {
                    var trashResponse = diskApiClient
                            .getTrashResources()
                            .then()
                            .statusCode(200)
                            .body("_embedded.items", notNullValue())
                            .extract()
                            .response();

                    List<Map<String, Object>> items =
                            trashResponse
                                    .jsonPath()
                                    .getList("_embedded.items");

                    Map<String, Object> trashItem = items.stream()
                            .filter(item ->
                                    originPath.equals(
                                            item.get("origin_path")
                                    )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new AssertionError(
                                            "Ресурс не найден в Корзине: "
                                                    + originPath
                                    )
                            );

                    String trashPath =
                            (String) trashItem.get("path");

                    Allure.step(
                            "Найден ресурс в Корзине: " + trashPath
                    );

                    return trashPath;
                }
        );
    }

    private void waitForOperation(
            String operationHref,
            String stepName
    ) {
        Allure.step(
                stepName,
                () -> Awaitility.await()
                        .atMost(Duration.ofSeconds(30))
                        .pollInterval(Duration.ofSeconds(1))
                        .untilAsserted(() ->
                                diskApiClient
                                        .getOperationStatus(operationHref)
                                        .then()
                                        .statusCode(200)
                                        .body(
                                                "status",
                                                equalTo("success")
                                        )
                        )
        );
    }
}