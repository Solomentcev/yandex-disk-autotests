package ru.yandex.disk.client;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import ru.yandex.disk.specs.RequestSpec;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

/**
 * Клиент REST API Яндекс Диска.
 */
public class DiskApiClient {

    /**
     * Получает информацию о Диске.
     *
     * @return HTTP-ответ API
     */
    @Step("Получить информацию о Диске")
    public Response getDiskInfo() {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get("/disk");
    }

    /**
     * Получает метаданные ресурса.
     *
     * @param path путь ресурса
     * @return HTTP-ответ API
     */
    @Step("Получить информацию о ресурсе: {path}")
    public Response getResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/resources");
    }

    /**
     * Создаёт каталог.
     *
     * @param path путь создаваемого каталога
     * @return HTTP-ответ API
     */
    @Step("Создать каталог: {path}")
    public Response createDirectory(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .put("/disk/resources");
    }

    /**
     * Удаляет ресурс.
     *
     * @param path путь ресурса
     * @param permanently признак окончательного удаления
     * @return HTTP-ответ API
     */
    @Step("Удалить ресурс: {path}, окончательно: {permanently}")
    public Response deleteResource(String path, boolean permanently) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .queryParam("permanently", permanently)
                .when()
                .delete("/disk/resources");
    }

    /**
     * Перемещает ресурс в Корзину.
     *
     * @param path путь ресурса
     * @return HTTP-ответ API
     */
    @Step("Удалить ресурс в Корзину: {path}")
    public Response deleteResource(String path) {
        return deleteResource(path, false);
    }

    /**
     * Получает статус асинхронной операции.
     *
     * @param operationHref ссылка на операцию
     * @return HTTP-ответ API
     */
    @Step("Получить статус асинхронной операции")
    public Response getOperationStatus(String operationHref) {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get(operationHref);
    }

    /**
     * Получает ссылку для загрузки файла.
     *
     * @param path путь загружаемого файла
     * @return HTTP-ответ API
     */
    @Step("Получить ссылку на загрузку файла: {path}")
    public Response getUploadLink(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/resources/upload");
    }

    /**
     * Загружает локальный файл по полученной ссылке.
     *
     * @param uploadHref ссылка для загрузки
     * @param file локальный файл
     * @return HTTP-ответ API
     */
    @Step("Загрузить файл: {file}")
    public Response uploadFile(String uploadHref, Path file) {
        return given(RequestSpec.uploadSpec())
                .multiPart("file", file.toFile())
                .when()
                .put(uploadHref);
    }

    /**
     * Получает временную ссылку для скачивания файла.
     *
     * @param path путь файла
     * @return HTTP-ответ API
     */
    @Step("Получить ссылку на скачивание файла: {path}")
    public Response getDownloadLink(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/resources/download");
    }

    /**
     * Скачивает файл по готовой временной ссылке.
     * Повторное URL-кодирование отключено, поскольку href уже возвращается
     * API в готовом виде.
     *
     * @param downloadHref готовая ссылка на скачивание
     * @return HTTP-ответ с содержимым файла
     */
    @Step("Скачать файл")
    public Response downloadFile(String downloadHref) {
        return given()
                .urlEncodingEnabled(false)
                .when()
                .get(downloadHref)
                .then()
                .extract()
                .response();
    }

    /**
     * Изменяет пользовательские свойства ресурса.
     *
     * @param path путь ресурса
     * @param customProperties пользовательские свойства
     * @return HTTP-ответ API
     */
    @Step("Изменить пользовательские свойства ресурса: {path}")
    public Response updateCustomProperties(String path, Map<String, Object> customProperties) {
        String propertiesJson = customProperties.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(","));

        String requestBody = "{\"custom_properties\":{" + propertiesJson + "}}";

        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/disk/resources");
    }

    /**
     * Получает список последних загруженных файлов.
     *
     * @return HTTP-ответ API
     */
    @Step("Получить список последних загруженных файлов")
    public Response getUploadedFiles() {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get("/disk/resources/files");
    }

    /**
     * Получает список ресурсов в Корзине.
     *
     * @return HTTP-ответ API
     */
    @Step("Получить список ресурсов в Корзине")
    public Response getTrashResources() {
        return given(RequestSpec.authorizedSpec())
                .when()
                .get("/disk/trash/resources");
    }

    /**
     * Получает ресурс из Корзины.
     *
     * @param path путь ресурса в Корзине
     * @return HTTP-ответ API
     */
    @Step("Получить ресурс из Корзины: {path}")
    public Response getTrashResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .get("/disk/trash/resources");
    }

    /**
     * Восстанавливает ресурс из Корзины.
     *
     * @param path путь ресурса в Корзине
     * @return HTTP-ответ API
     */
    @Step("Восстановить ресурс из Корзины: {path}")
    public Response restoreTrashResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .put("/disk/trash/resources/restore");
    }

    /**
     * Восстанавливает ресурс из Корзины с указанным именем.
     *
     * @param path путь ресурса в Корзине
     * @param name имя восстановленного ресурса
     * @return HTTP-ответ API
     */
    @Step("Восстановить ресурс из Корзины: {path}, имя: {name}")
    public Response restoreTrashResource(String path, String name) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .queryParam("name", name)
                .when()
                .put("/disk/trash/resources/restore");
    }

    /**
     * Окончательно удаляет ресурс из Корзины.
     *
     * @param path путь ресурса в Корзине
     * @return HTTP-ответ API
     */
    @Step("Окончательно удалить ресурс из Корзины: {path}")
    public Response deleteTrashResource(String path) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .when()
                .delete("/disk/thrash/resources");
    }

    /**
     * Загружает файл из внешнего URL.
     *
     * @param path путь создаваемого файла
     * @param url внешний URL
     * @return HTTP-ответ API
     */
    @Step("Загрузить файл из внешнего URL: {path}")
    public Response uploadFileFromUrl(String path, String url) {
        return given(RequestSpec.authorizedSpec())
                .queryParam("path", path)
                .queryParam("url", url)
                .when()
                .post("/disk/resources/upload");
    }
}