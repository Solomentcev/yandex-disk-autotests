# Yandex Disk API Autotests

Автотесты REST API сервиса Яндекс Диск.

## Стек

* Java 17
* JUnit 5
* REST Assured
* Allure
* Awaitility
* Maven
* Logback

## Покрытие

* `GET` — информация о диске и ресурсах;
* `POST` — загрузка файла;
* `PUT` — создание директории;
* `DELETE` — удаление ресурсов;
* работа с корзиной;
* проверка метаданных ресурсов;
* асинхронные операции API.

## Структура

```text
src/test/java/ru/yandex/disk
├── client
├── config
├── data
├── extensions
├── filters
├── specs
└── tests
```

## Настройка

Токен не хранится в репозитории.

Перед запуском необходимо задать переменную окружения:

```bash
export YANDEX_DISK_TOKEN="your-token"
```

## Запуск

```bash
mvn clean test
```

Allure Report:

```bash
mvn allure:serve
```
