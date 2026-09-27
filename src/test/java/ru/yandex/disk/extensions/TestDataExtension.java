package ru.yandex.disk.extensions;

import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.config.TestConfig;
import ru.yandex.disk.data.TestData;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Подготавливает и очищает инфраструктурные тестовые данные
 * Яндекс Диска.
 */
public class TestDataExtension
        implements BeforeEachCallback, AfterEachCallback {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TestDataExtension.class);

    private final DiskApiClient diskApiClient =
            new DiskApiClient();

    /**
     * Подготавливает инфраструктуру перед тестом.
     *
     * @param context контекст JUnit
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        Allure.step(
                "Подготовить инфраструктуру теста",
                this::ensureTestRootExists
        );
    }

    /**
     * Очищает тестовые данные после теста.
     *
     * @param context контекст JUnit
     */
    @Override
    public void afterEach(ExtensionContext context) {
        try {
            cleanupRemoteTestData();
        } finally {
            try {
                cleanupLocalTestFile();
            } finally {
                clearTestData();
            }
        }
    }

    /**
     * Проверяет наличие общего каталога для тестов.
     */
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

                    LOGGER.debug(
                            "Проверка общего каталога {}: HTTP {}",
                            rootPath,
                            statusCode
                    );

                    if (statusCode == 404) {
                        createTestRoot(rootPath);
                        return;
                    }

                    if (statusCode != 200) {
                        throw new AssertionError(
                                "Не удалось проверить общий каталог "
                                        + rootPath
                                        + ". HTTP: "
                                        + statusCode
                        );
                    }

                    LOGGER.debug(
                            "Общий каталог уже существует: {}",
                            rootPath
                    );
                }
        );
    }

    /**
     * Создаёт общий каталог для тестов.
     *
     * @param rootPath путь к общему каталогу
     */
    private void createTestRoot(String rootPath) {
        Allure.step(
                "Создать общий каталог: " + rootPath,
                () -> {
                    diskApiClient
                            .createDirectory(rootPath)
                            .then()
                            .statusCode(201);

                    LOGGER.info(
                            "Создан общий каталог: {}",
                            rootPath
                    );
                }
        );
    }

    /**
     * Очищает удалённые данные текущего теста.
     */
    private void cleanupRemoteTestData() {
        Allure.step(
                "Очистить удалённые тестовые данные",
                () -> {
                    if (TestData.hasCurrentTestPath()) {
                        String testPath =
                                TestData.currentTestPath();

                        cleanupTrash(testPath);
                        deleteTestDirectory(testPath);
                    }

                    deleteTestRoot();
                }
        );
    }

    /**
     * Удаляет оставшиеся ресурсы текущего теста из Корзины.
     *
     * @param testPath путь каталога текущего теста
     */
    private void cleanupTrash(String testPath) {
        Allure.step(
                "Очистить оставшиеся ресурсы теста из Корзины",
                () -> {
                    Response response =
                            diskApiClient.getTrashResources();

                    int statusCode = response.statusCode();

                    LOGGER.debug(
                            "Проверка Корзины перед очисткой: HTTP {}",
                            statusCode
                    );

                    if (statusCode != 200) {
                        throw new AssertionError(
                                "Не удалось получить содержимое "
                                        + "Корзины. HTTP: "
                                        + statusCode
                        );
                    }

                    List<Map<String, Object>> items =
                            response
                                    .jsonPath()
                                    .getList("_embedded.items");

                    if (items == null || items.isEmpty()) {
                        LOGGER.debug(
                                "В Корзине нет ресурсов"
                        );
                        return;
                    }

                    String expectedOriginPrefix =
                            "disk:" + testPath + "/";

                    for (Map<String, Object> item : items) {
                        Object originPath =
                                item.get("origin_path");

                        Object trashPath =
                                item.get("path");

                        if (originPath == null
                                || trashPath == null) {
                            continue;
                        }

                        if (originPath.toString()
                                .startsWith(expectedOriginPrefix)) {

                            deleteTrashResource(
                                    trashPath.toString()
                            );
                        }
                    }
                }
        );
    }

    /**
     * Окончательно удаляет ресурс из Корзины.
     *
     * @param trashPath путь ресурса в Корзине
     */
    private void deleteTrashResource(String trashPath) {
        Allure.step(
                "Окончательно удалить ресурс из Корзины: "
                        + trashPath,
                () -> {
                    Response response =
                            diskApiClient
                                    .deleteTrashResource(trashPath);

                    int statusCode = response.statusCode();

                    LOGGER.debug(
                            "Удаление ресурса из Корзины {}: HTTP {}",
                            trashPath,
                            statusCode
                    );

                    if (statusCode == 204) {
                        LOGGER.info(
                                "Ресурс удалён из Корзины: {}",
                                trashPath
                        );
                        return;
                    }

                    if (statusCode == 202) {
                        String operationHref = response
                                .jsonPath()
                                .getString("href");

                        waitForOperation(operationHref);

                        LOGGER.info(
                                "Ресурс асинхронно удалён из Корзины: {}",
                                trashPath
                        );
                        return;
                    }

                    if (statusCode == 404) {
                        LOGGER.debug(
                                "Ресурс уже отсутствует в Корзине: {}",
                                trashPath
                        );
                        return;
                    }

                    throw new AssertionError(
                            "Не удалось удалить ресурс из Корзины "
                                    + trashPath
                                    + ". HTTP: "
                                    + statusCode
                    );
                }
        );
    }

    /**
     * Окончательно удаляет каталог текущего теста.
     *
     * @param testPath путь каталога
     */
    private void deleteTestDirectory(String testPath) {
        Allure.step(
                "Окончательно удалить тестовый каталог: "
                        + testPath,
                () -> {
                    Response response =
                            diskApiClient
                                    .deleteResource(
                                            testPath,
                                            true
                                    );

                    int statusCode = response.statusCode();

                    LOGGER.debug(
                            "Удаление тестового каталога {}: HTTP {}",
                            testPath,
                            statusCode
                    );

                    if (statusCode == 404) {
                        LOGGER.debug(
                                "Тестовый каталог уже отсутствует: {}",
                                testPath
                        );
                        return;
                    }

                    if (statusCode == 204) {
                        LOGGER.info(
                                "Тестовый каталог удалён: {}",
                                testPath
                        );
                        return;
                    }

                    if (statusCode == 202) {
                        String operationHref = response
                                .jsonPath()
                                .getString("href");

                        LOGGER.debug(
                                "Удаление каталога выполняется "
                                        + "асинхронно: {}",
                                operationHref
                        );

                        waitForOperation(operationHref);

                        LOGGER.info(
                                "Тестовый каталог удалён: {}",
                                testPath
                        );
                        return;
                    }

                    throw new AssertionError(
                            "Не удалось удалить тестовый каталог "
                                    + testPath
                                    + ". HTTP: "
                                    + statusCode
                    );
                }
        );
    }

    /**
     * Окончательно удаляет общий каталог тестов.
     */
    private void deleteTestRoot() {
        String rootPath = TestData.testRootPath();

        Allure.step(
                "Окончательно удалить общий каталог: " + rootPath,
                () -> {
                    Response response =
                            diskApiClient.deleteResource(
                                    rootPath,
                                    true
                            );

                    int statusCode = response.statusCode();

                    LOGGER.debug(
                            "Удаление общего каталога {}: HTTP {}",
                            rootPath,
                            statusCode
                    );

                    if (statusCode == 404) {
                        LOGGER.debug(
                                "Общий каталог уже отсутствует: {}",
                                rootPath
                        );
                        return;
                    }

                    if (statusCode == 204) {
                        LOGGER.info(
                                "Общий каталог удалён: {}",
                                rootPath
                        );
                        return;
                    }

                    if (statusCode == 202) {
                        String operationHref =
                                response.jsonPath().getString("href");

                        LOGGER.debug(
                                "Удаление общего каталога выполняется "
                                        + "асинхронно: {}",
                                operationHref
                        );

                        waitForOperation(operationHref);

                        LOGGER.info(
                                "Общий каталог удалён: {}",
                                rootPath
                        );
                        return;
                    }

                    throw new AssertionError(
                            "Не удалось удалить общий каталог "
                                    + rootPath
                                    + ". HTTP: "
                                    + statusCode
                    );
                }
        );
    }

    /**
     * Ожидает завершения асинхронной операции.
     *
     * @param operationHref ссылка на операцию
     */
    private void waitForOperation(String operationHref) {
        Allure.step(
                "Дождаться завершения асинхронной операции",
                () -> Awaitility.await()
                        .atMost(
                                Duration.ofSeconds(
                                        TestConfig
                                                .getRequestTimeoutSeconds()
                                )
                        )
                        .pollInterval(Duration.ofSeconds(1))
                        .until(() -> {
                            Response response =
                                    diskApiClient
                                            .getOperationStatus(
                                                    operationHref
                                            );

                            String status = response
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .path("status");

                            LOGGER.debug(
                                    "Статус операции {}: {}",
                                    operationHref,
                                    status
                            );

                            if ("failed".equals(status)) {
                                throw new AssertionError(
                                        "Асинхронная операция "
                                                + "завершилась ошибкой: "
                                                + operationHref
                                );
                            }

                            return "success".equals(status);
                        })
        );
    }

    /**
     * Удаляет локальный временный файл.
     */
    private void cleanupLocalTestFile() {
        if (!TestData.hasCurrentTestFile()) {
            LOGGER.debug(
                    "Локальный тестовый файл отсутствует, "
                            + "очистка не требуется"
            );
            return;
        }

        Path file = TestData.currentTestFile();

        Allure.step(
                "Удалить локальный тестовый файл: " + file,
                () -> TestData.deleteTestFile(file)
        );
    }

    /**
     * Очищает ссылки на данные текущего теста.
     */
    private void clearTestData() {
        TestData.clearCurrentTestPath();
        TestData.clearCurrentTestFile();
    }
}