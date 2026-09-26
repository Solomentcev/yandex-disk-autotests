package ru.yandex.disk.filters;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Отображает HTTP-запросы и ответы как шаги Allure.
 *
 * <p>Заголовки и тела запросов и ответов намеренно не отображаются,
 * чтобы не перегружать отчёт и не выводить чувствительные данные.</p>
 */
public class AllureLoggingFilter implements Filter {

    @Override
    public Response filter(
            FilterableRequestSpecification requestSpec,
            FilterableResponseSpecification responseSpec,
            FilterContext context) {

        String requestLog = requestSpec.getMethod()
                + " "
                + requestSpec.getURI();

        Allure.step("HTTP request: " + requestLog);

        Response response;

        try {
            response = context.next(requestSpec, responseSpec);
        } catch (Exception e) {
            Allure.step(
                    "HTTP request failed: "
                            + requestLog
                            + " — "
                            + e.getMessage()
            );

            throw e;
        }

        String responseLog = response.getStatusCode()
                + " "
                + response.getStatusLine();

        Allure.step("HTTP response: " + responseLog);

        return response;
    }
}