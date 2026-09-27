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

    private static final String TEST_FILE_PREFIX = "test-file-";
    private static final String TEST_FILE_EXTENSION = ".txt";

    /**
     * Путь к каталогу текущего теста.
     */
    private static final ThreadLocal<String> CURRENT_TEST_PATH =
            new ThreadLocal<>();

    /**
     * Локальный файл текущего теста.
     */
    private static final ThreadLocal<Path> CURRENT_TEST_FILE =
            new ThreadLocal<>();

    private TestData() {
    }

    /**
     * Возвращает общий каталог для тестовых данных.
     *
     * @return путь к общему каталогу
     */
    public static String testRootPath() {
        return TestConfig.getTestRootPath();
    }

    /**
     * Генерирует уникальный путь для каталога текущего теста.
     *
     * @return уникальный путь
     */
    @Step("Сгенерировать уникальный путь тестового каталога")
    public static String uniqueRootPath() {
        String path = testRootPath() + "/" + UUID.randomUUID();

        CURRENT_TEST_PATH.set(path);

        LOGGER.info(
                "Сгенерирован путь тестового каталога: {}",
                path
        );

        return path;
    }

    /**
     * Создаёт локальный тестовый файл.
     *
     * @return путь к локальному файлу
     */
    @Step("Создать локальный тестовый файл")
    public static Path createTestFile() {
        try {
            Path file = Files.createTempFile(
                    TEST_FILE_PREFIX,
                    TEST_FILE_EXTENSION
            );

            String content =
                    "Yandex Disk autotest " + UUID.randomUUID();

            Files.writeString(file, content);

            CURRENT_TEST_FILE.set(file);

            LOGGER.info(
                    "Создан локальный тестовый файл: {}",
                    file
            );

            return file;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось создать локальный тестовый файл",
                    e
            );
        }
    }

    /**
     * Читает содержимое локального тестового файла.
     *
     * @param file локальный файл
     * @return содержимое файла
     */
    public static String readTestFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать локальный тестовый файл: "
                            + file,
                    e
            );
        }
    }

    /**
     * Возвращает размер локального тестового файла.
     *
     * @param file локальный файл
     * @return размер файла в байтах
     */
    public static long testFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось получить размер локального тестового файла: "
                            + file,
                    e
            );
        }
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
     * Проверяет наличие локального файла текущего теста.
     *
     * @return true, если файл сохранён
     */
    public static boolean hasCurrentTestFile() {
        return CURRENT_TEST_FILE.get() != null;
    }

    /**
     * Возвращает локальный файл текущего теста.
     *
     * @return путь к локальному файлу
     */
    public static Path currentTestFile() {
        Path file = CURRENT_TEST_FILE.get();

        if (file == null) {
            throw new IllegalStateException(
                    "Локальный тестовый файл не был создан"
            );
        }

        return file;
    }

    /**
     * Удаляет локальный тестовый файл.
     *
     * @param file локальный файл
     */
    @Step("Удалить локальный тестовый файл: {file}")
    public static void deleteTestFile(Path file) {
        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);

            LOGGER.info(
                    "Удалён локальный тестовый файл: {}",
                    file
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось удалить локальный тестовый файл: "
                            + file,
                    e
            );
        }
    }

    /**
     * Очищает сохранённый путь текущего теста.
     */
    public static void clearCurrentTestPath() {
        String path = CURRENT_TEST_PATH.get();

        if (path != null) {
            LOGGER.debug(
                    "Очищена ссылка на путь текущего теста: {}",
                    path
            );
        }

        CURRENT_TEST_PATH.remove();
    }

    /**
     * Очищает сохранённую ссылку на локальный файл.
     */
    public static void clearCurrentTestFile() {
        Path file = CURRENT_TEST_FILE.get();

        if (file != null) {
            LOGGER.debug(
                    "Очищена ссылка на локальный тестовый файл: {}",
                    file
            );
        }

        CURRENT_TEST_FILE.remove();
    }
}