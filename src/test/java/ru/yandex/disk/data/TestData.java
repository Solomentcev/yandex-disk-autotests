package ru.yandex.disk.data;

import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.disk.config.TestConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Хранит и генерирует тестовые данные.
 */
public final class TestData {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TestData.class);

    private static final String TEST_FILE_NAME = "test-file.txt";

    private static final ThreadLocal<String> CURRENT_TEST_PATH =
            new ThreadLocal<>();

    private TestData() {
    }

    /**
     * Возвращает общий каталог для тестовых данных.
     *
     * @return путь к корневому каталогу тестов
     */
    public static String testRootPath() {
        return TestConfig.getTestRootPath();
    }

    /**
     * Генерирует уникальный каталог для текущего теста.
     *
     * @return уникальный путь тестового каталога
     */
    @Step("Сгенерировать уникальный путь для тестовых данных")
    public static String uniqueRootPath() {
        String path = testRootPath() + "/" + UUID.randomUUID();

        LOGGER.info("Сгенерирован путь тестовых данных: {}", path);

        return path;
    }

    /**
     * Возвращает путь к тестовому файлу внутри текущего каталога.
     *
     * @return путь тестового файла на Яндекс Диске
     */
    public static String uniqueFilePath() {
        return currentTestPath() + "/" + TEST_FILE_NAME;
    }

    /**
     * Создаёт локальный тестовый файл.
     *
     * @return путь к созданному локальному файлу
     */
    @Step("Создать локальный тестовый файл")
    public static Path createTestFile() {
        try {
            Path file = Files.createTempFile(
                    "yandex-disk-test-",
                    ".txt"
            );

            String content = "Yandex Disk autotest " + UUID.randomUUID();

            Files.writeString(file, content);

            LOGGER.info("Создан локальный тестовый файл: {}", file);

            return file;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось создать локальный тестовый файл",
                    e
            );
        }
    }

    /**
     * Возвращает имя тестового файла.
     *
     * @return имя тестового файла
     */
    public static String testFileName() {
        return TEST_FILE_NAME;
    }

    /**
     * Сохраняет путь текущего теста.
     *
     * @param path путь тестового каталога
     */
    @Step("Сохранить путь тестовых данных: {path}")
    public static void setCurrentTestPath(String path) {
        CURRENT_TEST_PATH.set(path);

        LOGGER.info("Сохранён путь тестовых данных: {}", path);
    }

    /**
     * Проверяет наличие пути текущего теста.
     *
     * @return true, если путь сохранён
     */
    public static boolean hasCurrentTestPath() {
        return CURRENT_TEST_PATH.get() != null;
    }

    /**
     * Возвращает путь текущего теста.
     *
     * @return путь тестового каталога
     */
    public static String currentTestPath() {
        String path = CURRENT_TEST_PATH.get();

        if (path == null) {
            throw new IllegalStateException(
                    "Тестовый путь не был создан"
            );
        }

        return path;
    }

    /**
     * Очищает путь текущего теста.
     */
    @Step("Очистить данные текущего теста")
    public static void clearCurrentTestPath() {
        String path = CURRENT_TEST_PATH.get();

        if (path != null) {
            LOGGER.info("Очищен путь текущего теста: {}", path);
        }

        CURRENT_TEST_PATH.remove();
    }
}