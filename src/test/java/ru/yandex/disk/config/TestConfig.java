package ru.yandex.disk.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загружает конфигурацию тестового окружения.
 *
 * <p>Настройки загружаются из файла {@code test.properties}.
 * OAuth-токен берётся из переменной окружения,
 * если в конфигурации указана ссылка на переменную.</p>
 */
public final class TestConfig {

    private static final String CONFIG_FILE = "test.properties";

    private static final Properties PROPERTIES = loadProperties();

    private TestConfig() {
    }

    /**
     * Возвращает базовый URL API.
     *
     * @return базовый URL
     */
    public static String getBaseUrl() {
        return getRequiredProperty("base.url");
    }

    /**
     * Возвращает версию API.
     *
     * @return версия API
     */
    public static String getApiVersion() {
        return getRequiredProperty("api.version");
    }

    /**
     * Возвращает корневой каталог тестовых данных.
     *
     * @return путь к корневому каталогу
     */
    public static String getTestRootPath() {
        return getRequiredProperty("test.root.path");
    }

    /**
     * Возвращает таймаут ожидания асинхронных операций.
     *
     * @return таймаут в секундах
     */
    public static int getRequestTimeoutSeconds() {
        return Integer.parseInt(
                getRequiredProperty("request.timeout.seconds")
        );
    }

    /**
     * Возвращает OAuth-токен.
     *
     * <p>Если значение имеет формат {@code ${ENV_NAME}},
     * токен берётся из соответствующей переменной окружения.</p>
     *
     * @return OAuth-токен
     */
    public static String getToken() {
        String tokenProperty = getRequiredProperty("token");

        if (tokenProperty.startsWith("${")
                && tokenProperty.endsWith("}")) {

            String environmentVariable = tokenProperty.substring(
                    2,
                    tokenProperty.length() - 1
            );

            String token = System.getenv(environmentVariable);

            if (token == null || token.isBlank()) {
                throw new IllegalStateException(
                        "Переменная окружения "
                                + environmentVariable
                                + " не задана"
                );
            }

            return token;
        }

        return tokenProperty;
    }

    /**
     * Загружает файл конфигурации.
     *
     * @return загруженные свойства
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = TestConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Файл конфигурации не найден: "
                                + CONFIG_FILE
                );
            }

            properties.load(inputStream);
            return properties;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось загрузить файл конфигурации: "
                            + CONFIG_FILE,
                    e
            );
        }
    }

    /**
     * Возвращает обязательное свойство.
     *
     * @param key имя свойства
     * @return значение свойства
     */
    private static String getRequiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Не задано свойство конфигурации: " + key
            );
        }

        return value.trim();
    }
}