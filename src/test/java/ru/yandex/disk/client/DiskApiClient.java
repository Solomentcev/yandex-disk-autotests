package ru.yandex.disk.client;

import io.restassured.response.Response;
import ru.yandex.disk.specs.RequestSpec;

import java.nio.file.Path;

import static io.restassured.RestAssured.given;

/**
 * Клиент для взаимодействия с API Яндекс Диска.
 */
public class DiskApiClient {

    /**
     * Получает информацию о Диске.
     *
     * @return HTTP-ответ
     */
    public Response getDiskInfo() {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get("/disk");
    }

    /**
     * Получает информацию о ресурсе.
     *
     * @param path путь к ресурсу
     * @return HTTP-ответ
     */
    public Response getResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/resources");
    }

    /**
     * Создаёт каталог.
     *
     * @param path путь к каталогу
     * @return HTTP-ответ
     */
    public Response createDirectory(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .put("/disk/resources");
    }

    /**
     * Удаляет ресурс.
     *
     * @param path путь к ресурсу
     * @param permanently выполнять ли окончательное удаление
     * @return HTTP-ответ
     */
    public Response deleteResource(String path, boolean permanently) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .queryParam("permanently", permanently)
                .when()
                .delete("/disk/resources");
    }

    /**
     * Удаляет ресурс без окончательного удаления.
     *
     * @param path путь к ресурсу
     * @return HTTP-ответ
     */
    public Response deleteResource(String path) {
        return deleteResource(path, false);
    }

    /**
     * Получает статус асинхронной операции.
     *
     * @param operationHref URL операции
     * @return HTTP-ответ
     */
    public Response getOperationStatus(String operationHref) {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get(operationHref);
    }

    /**
     * Получает временную ссылку для загрузки файла.
     *
     * @param path путь, по которому будет размещён файл
     * @return HTTP-ответ
     */
    public Response getUploadLink(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/resources/upload");
    }

    /**
     * Загружает локальный файл по временной ссылке.
     *
     * @param uploadHref временная ссылка для загрузки
     * @param file локальный файл
     * @return HTTP-ответ
     */
    public Response uploadFile(String uploadHref, Path file) {
        return given(RequestSpec.uploadSpec())
                .multiPart("file", file.toFile())
                .when()
                .put(uploadHref);
    }
    /**
     * Получает временную ссылку для скачивания файла.
     *
     * @param path путь к файлу
     * @return HTTP-ответ
     */
    public Response getDownloadLink(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when().log().all()
                .get("/disk/resources/download");
    }

    /**
     * Скачивает файл по временной ссылке.
     *
     * @param downloadHref временная ссылка для скачивания
     * @return HTTP-ответ
     */
    public Response downloadFile(String downloadHref) {
        return given()
                .urlEncodingEnabled(false)
                .log().all()
                .when()
                .get(downloadHref)
                .then()
                .log().all()
                .extract()
                .response();
    }
    /**
     * Получает список ресурсов в Корзине.
     *
     * @return HTTP-ответ
     */
    public Response getTrashResources() {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get("/disk/trash/resources");
    }

    /**
     * Получает ресурс из Корзины.
     *
     * @param path исходный путь к ресурсу
     * @return HTTP-ответ
     */
    public Response getTrashResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/trash/resources");
    }

    /**
     * Окончательно удаляет ресурс из Корзины.
     *
     * @param path путь к ресурсу
     * @return HTTP-ответ
     */
    public Response deleteTrashResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .delete("/disk/trash/resources");
    }
}