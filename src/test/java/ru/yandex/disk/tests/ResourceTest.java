package ru.yandex.disk.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.config.TestConfig;
import ru.yandex.disk.data.TestData;
import ru.yandex.disk.extensions.TestDataExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Тесты работы с ресурсами Яндекс Диска.
 */
class ResourceTest {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    @Test
    @Tag("smoke")
    @Story("Получение ресурса")
    @Description("Проверка получения информации о корневом ресурсе Диска")
    void shouldGetResource() {

        Allure.step(
                "Получить и проверить информацию о корневом ресурсе: /",
                () -> diskApiClient
                        .getResource("/")
                        .then()
                        .statusCode(200)
                        .body("type", equalTo("dir"))
                        .body("path", equalTo("disk:/"))
        );
    }

    @Test
    @Tag("regression")
    @ExtendWith(TestDataExtension.class)
    @Story("Создание каталога")
    @Description("Проверка создания каталога и получения его метаданных")
    void shouldCreateDirectory() {

        String path = TestData.currentTestPath() + "/test-directory";

        Allure.step(
                "Создать каталог: " + path,
                () -> diskApiClient
                        .createDirectory(path)
                        .then()
                        .statusCode(201)
        );

        Allure.step(
                "Получить и проверить созданный каталог: " + path,
                () -> diskApiClient
                        .getResource(path)
                        .then()
                        .statusCode(200)
                        .body("path", equalTo("disk:" + path))
                        .body("type", equalTo("dir"))
                        .body(
                                "name",
                                equalTo("test-directory")
                        )
        );
    }

    @Test
    @Tag("regression")
    @ExtendWith(TestDataExtension.class)
    @Story("Удаление ресурса")
    @Description("Проверка удаления тестового каталога")
    void shouldDeleteResource() {

        String path = TestData.currentTestPath();

        Allure.step(
                "Проверить наличие каталога перед удалением: " + path,
                () -> diskApiClient
                        .getResource(path)
                        .then()
                        .statusCode(200)
                        .body("type", equalTo("dir"))
        );

        Allure.step(
                "Удалить каталог: " + path,
                () -> {
                    var response = diskApiClient.deleteResource(path);

                    if (response.getStatusCode() == 202) {
                        String operationHref = response.jsonPath()
                                .getString("href");

                        Allure.step(
                                "Дождаться завершения удаления: " + path,
                                () -> await()
                                        .atMost(Duration.ofSeconds(
                                                        TestConfig.getRequestTimeoutSeconds()))
                                        .pollInterval(Duration.ofSeconds(1))
                                        .untilAsserted(() ->
                                                diskApiClient
                                                        .getOperationStatus(
                                                                operationHref
                                                        )
                                                        .then()
                                                        .statusCode(200)
                                                        .body(
                                                                "status",
                                                                equalTo("success")
                                                        )
                                        )
                        );
                    } else {
                        response.then().statusCode(204);
                    }
                }
        );

        Allure.step(
                "Проверить отсутствие каталога после удаления: " + path,
                () -> diskApiClient
                        .getResource(path)
                        .then()
                        .statusCode(404)
        );
    }

    @Test
    @Tag("regression")
    @ExtendWith(TestDataExtension.class)
    @Story("Загрузка и удаление файла")
    @Description("Проверка загрузки файла на Диск и его последующего удаления")
    void shouldUploadAndDeleteFile() throws IOException {

        String filePath = TestData.uniqueFilePath();
        Path localFile = TestData.createTestFile();

        try {
            String uploadHref = Allure.step(
                    "Получить ссылку для загрузки файла: " + filePath,
                    () -> diskApiClient
                            .getUploadLink(filePath)
                            .then()
                            .statusCode(200)
                            .body("href", notNullValue())
                            .extract()
                            .jsonPath()
                            .getString("href")
            );

            Allure.step(
                    "Загрузить файл: " + filePath,
                    () -> diskApiClient
                            .uploadFile(uploadHref, localFile)
                            .then()
                            .statusCode(201)
            );

            Allure.step(
                    "Получить и проверить загруженный файл: " + filePath,
                    () -> diskApiClient
                            .getResource(filePath)
                            .then()
                            .statusCode(200)
                            .body(
                                    "path",
                                    equalTo("disk:" + filePath)
                            )
                            .body("type", equalTo("file"))
                            .body(
                                    "name",
                                    equalTo(TestData.testFileName())
                            )
            );
            Allure.step(
                    "Проверить содержимое загруженного файла: " + filePath,
                    () -> {
                        byte[] expectedContent = Files.readAllBytes(localFile);

                        String downloadHref = diskApiClient
                                .getDownloadLink(filePath)
                                .then()
                                .statusCode(200)
                                .body("href", notNullValue())
                                .extract()
                                .jsonPath()
                                .getString("href");
                        byte[] actualContent = diskApiClient
                                .downloadFile(downloadHref)
                                .then()
                                .statusCode(200)
                                .extract()
                                .asByteArray();

                        assertArrayEquals(expectedContent, actualContent);
                    }
            );
            Allure.step(
                    "Удалить файл: " + filePath,
                    () -> {
                        var response = diskApiClient
                                .deleteResource(filePath);

                        if (response.getStatusCode() == 202) {
                            String operationHref = response.jsonPath()
                                    .getString("href");

                            Allure.step(
                                    "Дождаться завершения удаления файла: "
                                            + filePath,
                                    () -> await()
                                            .atMost(Duration.ofSeconds(30))
                                            .pollInterval(Duration.ofSeconds(1))
                                            .untilAsserted(() ->
                                                    diskApiClient
                                                            .getOperationStatus(
                                                                    operationHref
                                                            )
                                                            .then()
                                                            .statusCode(200)
                                                            .body(
                                                                    "status",
                                                                    equalTo(
                                                                            "success"
                                                                    )
                                                            )
                                            )
                            );
                        } else {
                            response.then().statusCode(204);
                        }
                    }
            );

            Allure.step(
                    "Проверить отсутствие файла после удаления: " + filePath,
                    () -> diskApiClient
                            .getResource(filePath)
                            .then()
                            .statusCode(404)
            );

        } finally {
            Files.deleteIfExists(localFile);
        }
    }
}