package ru.yandex.disk.specs;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import ru.yandex.disk.config.TestConfig;
import ru.yandex.disk.filters.AllureLoggingFilter;

/**
 * Формирует общие спецификации HTTP-запросов.
 */
public final class RequestSpec {

    private RequestSpec() {
    }

    /**
     * Возвращает базовую спецификацию без авторизации.
     *
     * @return спецификация HTTP-запроса
     */
    public static RequestSpecification defaultSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.getBaseUrl())
                .setBasePath("/" + TestConfig.getApiVersion())
                .setContentType(ContentType.JSON)
                .addFilter(new AllureLoggingFilter())
                .build();
    }

    /**
     * Возвращает спецификацию с авторизацией
     * токеном из переменной окружения.
     *
     * @return авторизованная спецификация
     */
    public static RequestSpecification authorizedSpec() {
        return withToken(TestConfig.getToken());
    }

    /**
     * Возвращает спецификацию с указанным OAuth-токеном.
     *
     * <p>Используется в том числе для негативных тестов
     * с невалидным или пустым токеном.</p>
     *
     * @param token OAuth-токен
     * @return спецификация HTTP-запроса
     */
    public static RequestSpecification withToken(String token) {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.getBaseUrl())
                .setBasePath("/" + TestConfig.getApiVersion())
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "OAuth " + token)
                .addFilter(new AllureLoggingFilter())
                .build();
    }

    /**
     * Возвращает спецификацию для загрузки файла
     * по временной ссылке.
     *
     * <p>OAuth-заголовок не добавляется, поскольку
     * временная ссылка уже содержит параметры доступа.</p>
     *
     * @return спецификация HTTP-запроса
     */
    public static RequestSpecification uploadSpec() {
        return new RequestSpecBuilder()
                .addFilter(new AllureLoggingFilter())
                .build();
    }
}