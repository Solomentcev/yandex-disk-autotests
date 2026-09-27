package ru.yandex.disk.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.disk.client.DiskApiClient;
import ru.yandex.disk.data.TestData;
import ru.yandex.disk.extensions.TestDataExtension;

import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты работы с ресурсами Яндекс Диска.
 */
@Feature("Ресурсы")
@ExtendWith(TestDataExtension.class)
class ResourceTest {

    private final DiskApiClient diskApiClient = new DiskApiClient();

    /**
     * YD-RES-001.
     * Проверяет полный жизненный цикл файлового ресурса:
     * создание каталога, загрузку файла, получение метаданных,
     * скачивание и проверку содержимого, изменение пользовательских
     * свойств и окончательное удаление файла и каталога.
     */
    @Test
    @Tag("regression")
    @Story("Работа с файловым ресурсом")
    @Description(
            "YD-RES-001 — Работа с файловым ресурсом. "
                    + "Проверка создания каталога и файлового ресурса, "
                    + "получения и проверки метаданных, скачивания файла "
                    + "с проверкой содержимого, изменения пользовательских "
                    + "свойств и окончательного удаления ресурса "
                    + "без помещения в Корзину."
    )
    void shouldCreateUploadUpdateAndDeleteFile() {

        String testPath = TestData.uniqueRootPath();

        diskApiClient
                .createDirectory(testPath)
                .then()
                .statusCode(201);

        Path localFile = TestData.createTestFile();
        String fileName = localFile.getFileName().toString();
        String filePath = testPath + "/" + fileName;

        diskApiClient
                .getResource(testPath)
                .then()
                .statusCode(200)
                .body("type", equalTo("dir"))
                .body("path", equalTo("disk:" + testPath));

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

        var fileMetadata = diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(200)
                .body("type", equalTo("file"))
                .body("name", equalTo(fileName))
                .body("path", equalTo("disk:" + filePath))
                .extract()
                .response();

        assertEquals(
                TestData.testFileSize(localFile),
                fileMetadata.jsonPath().getLong("size")
        );

        String downloadHref = diskApiClient
                .getDownloadLink(filePath)
                .then()
                .statusCode(200)
                .body("href", notNullValue())
                .extract()
                .path("href");

        String downloadedContent = diskApiClient
                .downloadFile(downloadHref)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertEquals(
                TestData.readTestFile(localFile),
                downloadedContent
        );

        diskApiClient
                .updateCustomProperties(
                        filePath,
                        Map.of(
                                "testKey",
                                "testValue"
                        )
                )
                .then()
                .statusCode(200);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(200)
                .body(
                        "custom_properties.testKey",
                        equalTo("testValue")
                );

        diskApiClient
                .deleteResource(filePath, true)
                .then()
                .statusCode(204);

        diskApiClient
                .getResource(filePath)
                .then()
                .statusCode(404);

        diskApiClient
                .deleteResource(testPath, true)
                .then()
                .statusCode(204);

        diskApiClient
                .getResource(testPath)
                .then()
                .statusCode(404);
    }

    /**
     * YD-RES-NEG-001.
     * Проверяет получение метаданных ресурса,
     * отсутствующего на Яндекс Диске.
     */
    @Test
    @Tag("regression")
    @Story("Получение несуществующего ресурса")
    @Description(
            "YD-RES-NEG-001 — Проверка получения метаданных "
                    + "ресурса, отсутствующего на Яндекс Диске."
    )
    void shouldReturnNotFoundForNonexistentResource() {

        diskApiClient
                .getResource("/autotests/nonexistent-resource")
                .then()
                .statusCode(404);
    }


    /**
     * YD-RES-NEG-003.
     * Проверяет обработку повторной попытки создания
     * уже существующего каталога.
     */
    @Test
    @Tag("regression")
    @Story("Повторное создание существующего каталога")
    @Description(
            "YD-RES-NEG-003 — Проверка обработки повторной попытки "
                    + "создания уже существующего каталога."
    )
    void shouldReturnConflictWhenCreatingExistingDirectory() {

        String path = TestData.uniqueRootPath();

        diskApiClient
                .createDirectory(path)
                .then()
                .statusCode(201);

        diskApiClient
                .createDirectory(path)
                .then()
                .statusCode(409);
    }

    /**
     * YD-RES-NEG-005.
     * Проверяет получение ссылки на скачивание
     * отсутствующего файла.
     */
    @Test
    @Tag("regression")
    @Story("Получение ссылки на скачивание несуществующего файла")
    @Description(
            "YD-RES-NEG-005 — Проверка получения ссылки "
                    + "на скачивание отсутствующего файла."
    )
    void shouldReturnNotFoundWhenDownloadFileDoesNotExist() {

        diskApiClient
                .getDownloadLink(
                        "/autotests/nonexistent-parent/test-file.txt"
                )
                .then()
                .statusCode(404);
    }

    /**
     * YD-RES-NEG-006.
     * Проверяет изменение пользовательских свойств
     * отсутствующего ресурса.
     */
    @Test
    @Tag("regression")
    @Story("Изменение свойств несуществующего ресурса")
    @Description(
            "YD-RES-NEG-006 — Проверка изменения пользовательских "
                    + "свойств отсутствующего ресурса."
    )
    void shouldReturnNotFoundWhenUpdatingPropertiesOfNonexistentResource() {

        diskApiClient
                .updateCustomProperties(
                        "/autotests/nonexistent-parent/test-file.txt",
                        Map.of(
                                "testKey",
                                "testValue"
                        )
                )
                .then()
                .statusCode(404);
    }
}