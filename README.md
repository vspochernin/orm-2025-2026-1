# Учебная платформа для онлайн курса

Итоговый проект «Учебная платформа для онлайн курса» по дисциплине "ORM-фреймворки для Java". 1-й семестр 2-го курса МИФИ ИИКС РПО (2025-2026 уч. г).

Подробное описание задания находится в файле [task.md](task.md)

## Описание

Данный проект представляет собой web-приложение для управления онлайн-курсами на базе Spring Boot и Hibernate (JPA). Приложение позволяет создавать курсы со структурой (модули, уроки), записывать студентов, назначать задания, проводить тестирование и оценивать результаты. Проект демонстрирует работу с JPA/Hibernate, различные типы связей (1-1, 1-M, M-M), ленивую загрузку (а также проблемы с ней) и каскадные операции.

## Стек технологий

- **Java 21**.
- **Spring Boot 3.5.7**.
  - Spring Web (REST API).
  - Spring Data JPA (Hibernate).
  - Validation (для валидации и ограничений).
  - Testcontainers (для интеграционных тестов).
  - Lombok (для уменьшения boilerplate кода).
- **PostgreSQL 16** (СУБД)
- **Maven** (сборка, запуск и тестирование через `./mvnw`).

## Предусловия

- Java 21.
- Docker и Docker Compose (для PostgreSQL в dev-режиме)
- WSL2/Ubuntu (рекомендуется) или любая Unix-подобная система (PS. в теории можно запускать и на Windows, однако способ не проверялся).

## Запуск приложения (dev-профиль)

### 1. Запустить PostgreSQL

```bash
docker compose up -d
```

Это поднимет PostgreSQL 16 на `localhost:5432` с базой `gigalearn_dev` в фоновом процессе (креды для входа можно найти в `application-dev.yml` файле).

### 2. Запустить само приложение

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

При этом обязательно нужно указать профиль `dev`, ведь именно в нем у нас настроена `gigalearn_dev` база, запущенная на прошлом шаге.

При первом запуске:
- Hibernate создаст схему БД (16 таблиц).
- `DevDataLoader` загрузит демо-данные (1 курс, 2 модуля, 4 урока, тест и т.д.).
- При повторных запусках демо-данные не дублируются (проверка пустоты курсов гарантирует идемпотентность).

После запуска приложение становится доступно на `http://localhost:8080`.

### Важные настройки

- `spring.jpa.open-in-view=false` - явное управление транзакциями.
- `spring.jpa.hibernate.ddl-auto=update` (только dev профиль) - автообновление схемы.
- Демо-данные загружаются только в профиле `dev` (с помощью аннотации `@Profile("dev")`).

## Запуск тестов

```bash
./mvnw -DskipTests=false test
```

- **Написано 47 интеграционных тестов** (service + controller + application).
- Тесты используют **Testcontainers** с PostgreSQL 16-alpine.
- Профиль `test` настроен таким образом, что схема создается с нуля (`ddl-auto=create`). При этом после завершения схема, конечно, сбрасывается.
- Контейнер поднимается и останавливается автоматически (работа Testcontainers).

## REST API

Приложение предоставляет REST API для основных операций.

### Курсы и контент

**Создание курса:**
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Основы Java",
  "description": "Курс для начинающих",
  "categoryId": 1,
  "teacherId": 1,
  "tagIds": [1, 2]
}'

# Ответ: 201 Created
# { "id": 2 }
```

**Получение курса:**
```bash
curl http://localhost:8080/api/courses/1

# Ответ: 200 OK
# {
#   "id": 1,
#   "title": "Основы Hibernate и JPA",
#   ...
#   "teacherName": "Иван Петров",
#   "tagNames": ["Java", "ORM"]
# }
```

**Добавление модуля:**
```bash
curl -X POST http://localhost:8080/api/courses/1/modules \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Модуль 1",
  "description": "Описание",
  "orderIndex": 1
}'

# Ответ: 201 Created
# { "id": 3 }
```

**Добавление урока:**
```bash
curl -X POST http://localhost:8080/api/modules/3/lessons \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Урок 1",
  "content": "Контент урока",
  "videoUrl": "https://example.com/video"
}'

# Ответ: 201 Created
# { "id": 5 }
```

### Запись на курс

**Записать студента:**
```bash
curl -X POST "http://localhost:8080/api/courses/1/enroll?userId=2"

# Ответ: 201 Created
# { "id": 1 }
# При повторной попытке: 409 Conflict
```

**Отписать студента:**
```bash
curl -X DELETE "http://localhost:8080/api/courses/1/enroll?userId=2"

# Ответ: 204 No Content
```

### Задания и решения

**Создание задания:**
```bash
curl -X POST http://localhost:8080/api/lessons/1/assignments \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Домашнее задание",
  "description": "Опишите концепции ORM",
  "maxScore": 100
}'

# Ответ: 201 Created
# { "id": 2 }
```

**Сдача задания:**
```bash
curl -X POST http://localhost:8080/api/assignments/2/submit \
  -H "Content-Type: application/json" \
  -d '{
  "studentId": 2,
  "content": "Моё решение..."
}'

# Ответ: 201 Created
# { "id": 1 }
# При повторной попытке: 409 Conflict
```

**Оценивание:**
```bash
curl -X POST http://localhost:8080/api/submissions/1/grade \
  -H "Content-Type: application/json" \
  -d '{
  "score": 85,
  "feedback": "Отличная работа!"
}'

# Ответ: 204 No Content
# Если score > maxScore: 400 Bad Request
```

### Квизы (тестирование знаний)

**Создание квиза:**
```bash
curl -X POST http://localhost:8080/api/modules/2/quiz \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Тест по модулю",
  "timeLimitSeconds": 1800
}'

# Ответ: 201 Created
# { "id": 2 }

# Примечание: модуль может иметь только один квиз (связь 1-1 optional).
# Для примера используется модуль 2, т.к. модуль 1 уже имеет квиз из демо-данных.
```

**Добавление вопроса:**
```bash
curl -X POST http://localhost:8080/api/quizzes/2/questions \
  -H "Content-Type: application/json" \
  -d '{
  "text": "Что такое ORM?"
}'

# Ответ: 201 Created
# { "id": 3 }
```

**Добавление варианта ответа:**
```bash
curl -X POST http://localhost:8080/api/questions/3/options \
  -H "Content-Type: application/json" \
  -d '{
  "text": "Object-Relational Mapping",
  "isCorrect": true
}'

# Ответ: 201 Created
# { "id": 7 }
```

**Прохождение теста:**
```bash
curl -X POST http://localhost:8080/api/quizzes/1/take \
  -H "Content-Type: application/json" \
  -d '{
  "studentId": 2,
  "answersByQuestion": {
    "1": [1],
    "2": [5]
  }
}'

# Ответ: 201 Created
# { "id": 1 }
```

Балл подсчитывается автоматически: вопрос считается верным, если выбранные варианты точно совпадают с правильными.

### Обработка ошибок

- **400 Bad Request** - ошибки валидации, не найдено.
- **409 Conflict** - нарушение уникальности (повторная запись/сдача).
- **500 Internal Server Error** - внутренние ошибки.

Пример ответа при ошибке:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-04T15:00:00Z",
  "fieldErrors": {
    "title": "Title is required"
  }
}
```

## Быстрый мануальный прогон

### Сценарий 1: Создание курса с контентом

```bash
# 1. Создать курс (нужны categoryId=1, teacherId=1 из демо-данных)
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"title":"Новый курс","categoryId":1,"teacherId":1}'
# Ответ: {"id":2}
```

```bash
# 2. Добавить модуль
curl -X POST http://localhost:8080/api/courses/2/modules \
  -H "Content-Type: application/json" \
  -d '{"title":"Модуль 1","orderIndex":1}'
# Ответ: {"id":3}

```bash
# 3. Добавить урок
curl -X POST http://localhost:8080/api/modules/3/lessons \
  -H "Content-Type: application/json" \
  -d '{"title":"Урок 1","content":"Контент"}'
# Ответ: {"id":5}
```

### Сценарий 2: Задание и оценка

```bash
# 1. Создать задание
curl -X POST http://localhost:8080/api/lessons/1/assignments \
  -H "Content-Type: application/json" \
  -d '{"title":"Задание 1","maxScore":100}'
# Ответ: {"id":2}
```

```bash
# 2. Студент сдает задание
curl -X POST http://localhost:8080/api/assignments/2/submit \
  -H "Content-Type: application/json" \
  -d '{"studentId":2,"content":"Мое решение"}'
# Ответ: {"id":1}
```

```bash
# 3. Оценить сдачу
curl -X POST http://localhost:8080/api/submissions/1/grade \
  -H "Content-Type: application/json" \
  -d '{"score":90,"feedback":"Хорошо!"}'
# Ответ: 204 No Content
```

### Сценарий 3: Квиз

```bash
# 1. Создать квиз для модуля (используем модуль 2, т.к. модуль 1 уже имеет квиз из демо-данных)
curl -X POST http://localhost:8080/api/modules/2/quiz \
  -H "Content-Type: application/json" \
  -d '{"title":"Тест","timeLimitSeconds":1800}'
# Ответ: {"id":2}
```

```bash
# 2. Добавить вопрос
curl -X POST http://localhost:8080/api/quizzes/2/questions \
  -H "Content-Type: application/json" \
  -d '{"text":"Что такое LAZY?"}'
# Ответ: {"id":3}
```

```bash
# 3. Добавить варианты (правильный и неправильный)
curl -X POST http://localhost:8080/api/questions/3/options \
  -H "Content-Type: application/json" \
  -d '{"text":"Ленивая загрузка","isCorrect":true}'
# Ответ: {"id":7}
```

```bash
curl -X POST http://localhost:8080/api/questions/3/options \
  -H "Content-Type: application/json" \
  -d '{"text":"Немедленная загрузка","isCorrect":false}'
# Ответ: {"id":8}
```

```bash
# 4. Студент проходит тест (вопрос 1 из демо-данных: правильный вариант = 1)
curl -X POST http://localhost:8080/api/quizzes/1/take \
  -H "Content-Type: application/json" \
  -d '{"studentId":2,"answersByQuestion":{"1":[1],"2":[5]}}'
# Ответ: {"id":1} (score подсчитывается автоматически)
```

## CI/CD

В проекте настроен GitHub Actions workflow (`.github/workflows/ci.yml`):
- Триггеры: push и pull_request на ветку `master`.
- Окружение: Ubuntu + Java 21 (Temurin) + Maven cache.
- Автоматический запуск всех тестов с Testcontainers.
- При ошибках сохраняются test reports как артефакты.

## Структура кода проекта

```
src/main/java/ru/vspochernin/gigalearn/
├── config/          DevDataLoader для загрузки демо-данных (для профиля dev)
├── controller/      REST контроллеры
├── dto/             Request/Response DTO с валидацией
├── entity/          JPA-сущности
├── exception/       Обработка ошибок
├── repository/      Spring Data JPA репозитории
└── service/         Бизнес-логика

src/test/java/ru/vspochernin/gigalearn/
├── controller/      REST-тесты с MockMvc
├── service/         Service-тесты с Testcontainers
└── GigalearnApplicationTests.java (контекстный тест).
```

**Ключевые слои:**
- **Entity** - доменная модель (User, Course, Module, Lesson, Assignment, Quiz и др.).
- **Repository** - доступ к данным через Spring Data JPA.
- **Service** - транзакционная бизнес-логика приложения.
- **Controller** - REST API эндпоинты.
- **DTO** - Data Transfer Object'ы.
- **Exception** - классы для обработки ошибок.

## Соответствие критериям

### Критерий 1. Модель данных: 15–20 сущностей, связи 1-1, 1-M, M-M (6 баллов)

**Выполнено:** Реализовано 15 JPA-сущностей (User, Profile, Category, Tag, Course, Module, Lesson, Assignment, Submission, Quiz, Question, AnswerOption, Enrollment, QuizSubmission, CourseReview) + 1 enum (Role). Между сущностями настроены все виды связей (один к одному, один ко многим, многие ко многим).

Все связи настроены с `fetch = LAZY` для демонстрации работы ленивой загрузки.

### Критерий 2. Репозитории и CRUD-операции (5 баллов)

**Выполнено:** 15 репозиториев на базе `JpaRepository`.

Все ключевые сущности имеют репозитории с базовыми CRUD и кастомными query-методами (`findByCourseId`, `findByStudentId`, `findByUserIdAndCourseId` и т.д.). Каскадные операции работают корректно.

### Критерий 3. Управление курсами и контентом (4 балла)

**Выполнено:** `CourseService`, `ModuleService`, `LessonService`.

Реализовано создание курсов с указанием категории и преподавателя, добавление модулей (с `orderIndex`) и уроков, обновление/удаление с каскадами. Метод `getCourseWithContent()` загружает курс со всеми зависимостями.

### Критерий 4. Запись на курс (3 балла)

**Выполнено:** `EnrollmentService`.

Функции: запись студента на курс (`enrollStudent`) с проверкой уникальности пары (user, course), отписка (`unenrollStudent`), получение списка курсов студента и студентов курса. UNIQUE constraint на (user_id, course_id) + обработка `DuplicateEnrollmentException`.

### Критерий 5. Задания и решения (3 балла)

**Выполнено:** `AssignmentService`, `SubmissionService`.

Полный цикл: преподаватель создает задание, студент сдает (`submit`), система проверяет уникальность (один студент - одна сдача), преподаватель оценивает (`grade` с валидацией score <= maxScore). Списки сдач доступны по заданию и по студенту.

### Критерий 6. Тесты/викторины (3 балла)

**Выполнено:** `QuizService`.

Реализованы сущности Quiz, Question, AnswerOption, QuizSubmission. Можно создать тест с вопросами и вариантами, студент проходит квиз (`takeQuiz`) с валидацией целостности (questionId принадлежит quizId, optionId принадлежит questionId). Балл подсчитывается автоматически: вопрос верен, если выбранные варианты точно совпадают с правильными (присутствует поддержка SINGLE_CHOICE и MULTIPLE_CHOICE). Множественные попытки разрешены.

### Критерий 7. Конфигурация приложения (PostgreSQL) (3 балла)

**Выполнено:** Профили `dev` и `test`.

Конфигурация вынесена в `application.yml`, `application-dev.yml`, `application-test.yml`. Чувствительные данные (пароли) не находятся в коде. Dev окружение использует PostgreSQL из `docker-compose.yml`. Test окружение использует Testcontainers с `@DynamicPropertySource`. Мною предполагается, что продовое окружение подключается к некой "продовой" базе данных, создание которой для демонстрации проекта - избыточною

### Критерий 8. Интеграционное тестирование CRUD (3 балла)

**Выполнено:** 27 service-тестов.

Используются `@SpringBootTest` + Testcontainers PostgreSQL. Покрыты сценарии: создание курса с модулями/уроками (каскад), чтение графа зависимостей, удаление (orphanRemoval), запись/отписка, сдача/оценивание, прохождение квиза. Отдельный тест на `LazyInitializationException`.

### Критерий 9. REST API для основных операций (5 баллов)

**Выполнено:** 14 эндпойнтов в 7 контроллерах.

Все операции доступны через REST: создание курсов/модулей/уроков, запись на курс, задания/сдачи/оценивание, квизы/вопросы/прохождение. Корректные HTTP-методы (POST/GET/DELETE) и коды ответов (201/200/204/400/409). Контроллеры используют DTO и не отдают JPA-сущности напрямую.

### Критерий 10. Валидация ввода и обработка ошибок (3 балла)

**Выполнено:** `GlobalExceptionHandler` + валидация DTO.

Все входные DTO имеют ограничения (`@NotBlank`, `@NotNull`, `@Positive`, `@PositiveOrZero`). Централизованная обработка идет через `@RestControllerAdvice`: валидация -> 400 с `fieldErrors`, IllegalArgumentException -> 400, дубликаты -> 409, generic -> 500. Сообщения понятные.

### Критерий 11. Предзаполнение данными (2 балла)

**Выполнено:** `DevDataLoader` с `@Profile("dev")`.

При первом запуске в dev-режиме загружаются демо-данные: 1 преподаватель, 2 студента, 1 курс с модулями и уроками, задание, квиз с вопросами. Присутствует идемпотентность: повторные запуски не дублируют данные (используется проверка `count() > 0` у курсов, которые точно будут не пустые, если демо-данные уже были загружены однажды).

### Критерий 12. Тестирование (unit + интеграционные) (3 балла)

**Выполнено:** 47 интеграционных тестов.

- Service тесты (27): CourseContentServiceTest, EnrollmentServiceTest, SubmissionServiceTest, QuizServiceTest.
- Controller тесты (19): CourseControllerTest, AssignmentControllerTest, QuizControllerTest.
- Application тест (1): GigalearnApplicationTests.

Все тесты структурированы, хорошо читаемы и успешно проходят. Покрытие: CRUD, ленивая загрузка, каскады, валидация, обработка ошибок (покрыта большая часть функциональности, процент покрытия внушительный).

### Критерий 13. Архитектура и качество кода (3 балла)

**Выполнено:** Многослойная архитектура.

Проект разделен на слои (пакеты): entity, repository, service, controller, dto, exception, config. Применяются принципы SOLID (например, DI через конструктор), DRY (например, helper-методы в тестах), чистые имена классов и методов. Также используется Lombok для уменьшения boilerplate кода.

### Критерий 14. Документация проекта (2 балла)

**Выполнено:** README.md с подробными инструкциями и описанием.

В проекте присутствует подробный README.md (который вы сейчас и читаете). Файл содержит описание стека, инструкции по запуску (`dev` профиль и тесты), список эндпойнтов с примерами запросов, описание архитектуры и соответствие критериям.

### Критерий 15. Автоматизация и DevOps (2 балла)

**Выполнено:** GitHub Actions CI + docker-compose.

Создан GitHub Workflow `.github/workflows/ci.yml`, который автоматически запускает тесты на каждый push или PR в `master` ветку. Также в корне проекта лежит `docker-compose.yml` для удобного поднятия dev PostgreSQL. Для работы Maven используется `./mvnw`, который шел "в комплекте" при инициализации проекта через Spring Initializr.

---

## Итого

**Базовые критерии: 43/43 балла**.

Все обязательные критерии были выполнены в полном объеме.
