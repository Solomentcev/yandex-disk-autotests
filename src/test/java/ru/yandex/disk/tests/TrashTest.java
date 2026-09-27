package ru.yandex.disk.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.config.TestConfig;
import ru.yandex.disk.data.TestData;
import ru.yandex.disk.extensions.TestDataExtension;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты работы с Корзиной Яндекс Диска.
 */
@Feature("Корзина")
@ExtendWith(TestDataExtension.class)
class TrashTest {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    /**
     * YD-TRASH-001.
     * Проверяет удаление файлового ресурса в Корзину,
     * сохранение исходного пути, восстановление ресурса
     * в исходное расположение и окончательное удаление.
     */
    @Test
    @Tag("regression")
    @Story("Работа с удалённым ресурсом в Корзине")
    @Description(
            "YD-TRASH-001 — Работа с удалённым ресурсом в Корзине. "
                    + "Проверка удаления файлового ресурса в Корзину, "
                    + "сохранения исходного пути, восстановления ресурса "
                    + "в исходное расположение и его окончательного удаления."
    )
    void shouldRestoreDeletedFileFromTrash() {

        String testPath = TestData.uniqueRootPath();

        diskApiClient
                .createDirectory(testPath)
                .then()
                .statusCode(201);

        Path localFile = TestData.createTestFile();
        String fileName = localFile.getFileName().toString();
        String filePath = testPath + "/" + fileName;

        String uploadHref = diskApiClient
                .getUploadLink(filePath)
                .then()
                .statusCode(200)
                .body("href", notNullValue())
                .extract()
                .path("href");

        diskApiClient
                .uploadFile(uploadHref, localFile)
                .then()
                .statusCode(201);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(200)
                .body("type", equalTo("file"))
                .body("name", equalTo(fileName))
                .body("path", equalTo("disk:" + filePath));

        Response deleteResponse =
                diskApiClient.deleteResource(filePath);

        handleDeleteOperation(deleteResponse);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(404);

        String trashPath = findResourceInTrash(filePath);

        diskApiClient
                .getTrashResource(trashPath)
                .then()
                .statusCode(200)
                .body(
                        "origin_path",
                        equalTo("disk:" + filePath)
                )
                .body(
                        "name",
                        equalTo(fileName)
                );

        Response restoreResponse =
                diskApiClient.restoreTrashResource(trashPath);

        handleRestoreOperation(restoreResponse);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(200)
                .body("type", equalTo("file"))
                .body("name", equalTo(fileName))
                .body("path", equalTo("disk:" + filePath));

        diskApiClient
                .deleteResource(filePath, true)
                .then()
                .statusCode(204);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(404);
    }

    /**
     * Находит ресурс текущего теста в Корзине.
     *
     * @param filePath исходный путь файла
     * @return путь ресурса в Корзине
     */
    @Step("Найти ресурс в Корзине по исходному пути: {filePath}")
    private String findResourceInTrash(String filePath) {

        String originPath = "disk:" + filePath;

        Response trashResponse = diskApiClient
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
                        originPath.equals(item.get("origin_path"))
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Ресурс не найден в Корзине: "
                                        + originPath
                        )
                );

        String trashPath = (String) trashItem.get("path");

        if (trashPath == null) {
            throw new AssertionError(
                    "У ресурса в Корзине отсутствует path: "
                            + originPath
            );
        }

        return trashPath;
    }

    /**
     * Обрабатывает результат перемещения ресурса в Корзину.
     *
     * @param response ответ API
     */
    private void handleDeleteOperation(Response response) {

        int statusCode = response.statusCode();

        if (statusCode == 204) {
            return;
        }

        if (statusCode == 202) {
            String operationHref =
                    response.jsonPath().getString("href");

            waitForOperation(operationHref);
            return;
        }

        throw new AssertionError(
                "Неожиданный HTTP статус удаления: "
                        + statusCode
                        + ". Ожидался 204 или 202."
        );
    }

    /**
     * Обрабатывает результат восстановления ресурса.
     *
     * @param response ответ API
     */
    private void handleRestoreOperation(Response response) {

        int statusCode = response.statusCode();

        if (statusCode == 201) {
            return;
        }

        if (statusCode == 202) {
            String operationHref =
                    response.jsonPath().getString("href");

            waitForOperation(operationHref);
            return;
        }

        throw new AssertionError(
                "Неожиданный HTTP статус восстановления: "
                        + statusCode
                        + ". Ожидался 201 или 202."
        );
    }

    /**
     * Ожидает завершения асинхронной операции.
     *
     * @param operationHref ссылка на операцию
     */
    @Step("Дождаться завершения асинхронной операции")
    private void waitForOperation(String operationHref) {

        Awaitility.await()
                .atMost(
                        Duration.ofSeconds(
                                TestConfig.getRequestTimeoutSeconds()
                        )
                )
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {

                    Response response =
                            diskApiClient
                                    .getOperationStatus(operationHref);

                    response.then().statusCode(200);

                    String status =
                            response.jsonPath()
                                    .getString("status");

                    if ("failed".equals(status)) {
                        throw new AssertionError(
                                "Асинхронная операция завершилась "
                                        + "ошибкой: "
                                        + operationHref
                        );
                    }

                    return "success".equals(status);
                });
    }
}