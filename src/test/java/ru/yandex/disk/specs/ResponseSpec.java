package ru.yandex.disk.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

/**
 * Формирует общие спецификации HTTP-ответов.
 */
public final class ResponseSpec {

    private ResponseSpec() {
    }

    /**
     * Возвращает спецификацию JSON-ответа.
     *
     * @return спецификация HTTP-ответа
     */
    public static ResponseSpecification jsonResponse() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }
}