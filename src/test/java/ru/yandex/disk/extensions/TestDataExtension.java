package ru.yandex.disk.extensions;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.config.TestConfig;
import ru.yandex.disk.data.TestData;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Подготавливает и очищает тестовые данные Яндекс Диска.
 */
public class TestDataExtension
        implements BeforeEachCallback, AfterEachCallback {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    @Override
    public void beforeEach(ExtensionContext context) {
        Allure.step(
                "Подготовить тестовые данные",
                () -> {
                    ensureTestRootExists();

                    String testPath = TestData.uniqueRootPath();

                    createTestDirectory(testPath);

                    TestData.setCurrentTestPath(testPath);
                }
        );
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!TestData.hasCurrentTestPath()) {
            return;
        }

        try {
            Allure.step(
                    "Очистить тестовые данные",
                    this::deleteTestDirectory
            );
        } finally {
            TestData.clearCurrentTestPath();
        }
    }

    private void ensureTestRootExists() {
        String rootPath = TestData.testRootPath();

        Allure.step(
                "Проверить общий каталог: " + rootPath,
                () -> {
                    int statusCode = diskApiClient
                            .getResource(rootPath)
                            .then()
                            .extract()
                            .statusCode();

                    if (statusCode == 404) {
                        Allure.step(
                                "Создать общий каталог: " + rootPath,
                                () -> diskApiClient
                                        .createDirectory(rootPath)
                                        .then()
                                        .statusCode(201)
                        );

                        return;
                    }

                    assertEquals(
                            200,
                            statusCode,
                            "Не удалось проверить общий каталог: "
                                    + rootPath
                    );
                }
        );
    }

    private void createTestDirectory(String testPath) {
        Allure.step(
                "Создать тестовый каталог: " + testPath,
                () -> diskApiClient
                        .createDirectory(testPath)
                        .then()
                        .statusCode(201)
        );
    }

    private void deleteTestDirectory() {
        String testPath = TestData.currentTestPath();

        Allure.step(
                "Удалить тестовый каталог: " + testPath,
                () -> {
                    var response = diskApiClient
                            .deleteResource(testPath, true);

                    int statusCode = response.statusCode();

                    if (statusCode == 404) {
                        return;
                    }

                    if (statusCode != 202 && statusCode != 204) {
                        throw new AssertionError(
                                "Ожидался статус 202, 204 или 404 "
                                        + "при очистке тестовых данных, "
                                        + "получен: " + statusCode
                        );
                    }

                    if (statusCode == 202) {
                        String operationHref =
                                response.jsonPath().getString("href");

                        waitForOperation(operationHref);
                    }
                }
        );
    }

    private void waitForOperation(String operationHref) {
        Allure.step(
                "Дождаться завершения удаления",
                () -> await()
                        .atMost(Duration.ofSeconds(
                                TestConfig.getRequestTimeoutSeconds()
                        ))
                        .pollInterval(Duration.ofSeconds(1))
                        .until(() -> {
                            String status = diskApiClient
                                    .getOperationStatus(operationHref)
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .path("status");

                            if ("failed".equals(status)) {
                                throw new AssertionError(
                                        "Асинхронная операция завершилась ошибкой"
                                );
                            }

                            return "success".equals(status);
                        })
        );
    }
}