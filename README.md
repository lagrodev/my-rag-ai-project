# RAG Backend

## Суть проекта

RAG Backend — это серверная часть системы для работы с документами на основе паттерна **Retrieval-Augmented Generation (RAG)**. Система позволяет загружать документы (PDF и другие форматы), автоматически извлекать из них структурированный текст, разбивать его на семантические фрагменты, индексировать с помощью векторных эмбеддингов и отвечать на вопросы пользователя, опираясь на содержимое конкретного документа.

Ключевая идея: вместо того чтобы дообучать языковую модель, система при каждом запросе находит в документе наиболее релевантные фрагменты и передаёт их в LLM как контекст. Это позволяет работать с закрытыми корпоративными документами без утечки данных и без переобучения модели.

---

## Общая информация

| Параметр | Значение |
|---|---|
| Название | `rag-backend` |
| Версия | `0.0.1-SNAPSHOT` |
| Группа | `ai.chat` |
| Java | 21 |
| Spring Boot | 3.x / 4.x |
| Сборка | Gradle 8 (Kotlin DSL), multi-module |

---

## Архитектура

Проект состоит из двух основных частей: **Java-бэкенд** (Spring Boot) и **Python-воркер** для разбора PDF-документов. Взаимодействие между ними осуществляется через Apache Kafka (Redpanda). Хранилище файлов — MinIO. Аутентификация — Keycloak (OAuth2/JWT).

### Структура Java-монорепозитория

```
backend/
└── core/
    ├── logging-common/          # Переиспользуемая библиотека логирования
    └── rag-service/
        ├── app/                 # Точка входа, конфигурация, Security
        ├── common/
        │   ├── api-contracts/   # Общие DTO и контракты (пока минимально)
        │   └── core-utils/      # AbstractEntity, исключения, FileStoragePort
        └── features/
            ├── document-manager/  # Загрузка, хранение, метаданные документов
            └── rag-core/          # Парсинг, индексация, поиск, чат
```

### Общая схема потока данных

```
Пользователь
    |
    | 1. Инициирует загрузку (init-upload)
    v
[rag-service] --> [MinIO] (presigned PUT URL)
    |
    | 2. Подтверждает загрузку (confirm-upload)
    v
[rag-service] --> publishes --> [Kafka: document-parse-tasks]
                                         |
                                         v
                               [doc-parser-python]
                               PDF -> Markdown -> JSON (дерево секций)
                               JSON --> [MinIO: processed-json-doc]
                               --> publishes --> [Kafka: json-outgoing]
                                         |
                                         v
                               [rag-service: KafkaEventListener]
                               Читает JSON из MinIO
                               Строит DocumentSection дерево
                               Разбивает на Chunk-и
                               Генерирует эмбеддинги
                               Сохраняет в PostgreSQL (pgvector)

Пользователь
    |
    | 3. Задаёт вопрос (POST /api/chat/ask)
    v
[rag-service: RagService]
    --> EmbeddingClient: эмбеддинг запроса
    --> ChunkRepository: top-K по cosine similarity (pgvector)
    --> Parent Document Retrieval: берём секцию целиком или чанк
    --> LlmClient: строим промпт + история диалога + контекст
    --> ChatResponse (ответ + источники)
```

---

## Модули и основные компоненты

### logging-common

Переиспользуемая Spring Boot Auto-configuration библиотека для единообразного логирования.

- `@Loggable` — аннотация для методов и классов. Включает AOP-перехват: логирует вход/выход, аргументы, результат, время выполнения. При превышении `slowThresholdMs` выдаёт предупреждение и инкрементирует метрику `logging.slow_requests.total` (Micrometer).
- `LoggingAspect` — реализация AOP-аспекта.
- `MdcContextFilter` / `ReactiveMdcContextFilter` — фильтры для заполнения MDC (requestId, userId и т.д.) для каждого входящего запроса.
- `MdcTaskDecorator` — декоратор задач для передачи MDC-контекста в асинхронные потоки.
- `SensitiveMaskingConverter` — Logback-конвертер для маскировки чувствительных данных в логах.
- `RequestLoggingFilter` / `ReactiveRequestLoggingFilter` — фильтры логирования HTTP-запросов и ответов.

### core-utils

Общие компоненты, используемые всеми модулями rag-service.

- `AbstractEntity` — базовый JPA-класс с полями `id` (UUID), `createdAt`, `updatedAt`.
- `GlobalExceptionHandler` — централизованный обработчик исключений (`@RestControllerAdvice`).
- `ApplicationException`, `AppError` — иерархия бизнес-исключений.
- `DocumentNotFoundException`, `NotFoundException`, `FileReadError` — конкретные исключения.
- `FileStoragePort` — порт (интерфейс) для абстракции над файловым хранилищем.
- `UploadFileEvent`, `PresignedUploadDto` — DTO для взаимодействия между модулями.

### document-manager

Управляет жизненным циклом документов со стороны пользователя.

**REST API** (`/api/documents`, требуется JWT):

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/documents` | Список документов пользователя (с пагинацией) |
| POST | `/api/documents/init-upload` | Инициализация загрузки: проверка дубликата по MD5, возврат presigned URL |
| POST | `/api/documents/confirm-upload` | Подтверждение загрузки: создание FileAsset, публикация события |
| GET | `/api/documents/{id}` | Метаданные документа |
| GET | `/api/documents/{id}/download` | Presigned URL для скачивания |
| GET | `/api/documents/search?filename=` | Поиск по имени файла |
| DELETE | `/api/documents/{id}` | Удаление документа |

**Двухэтапная загрузка с дедупликацией:** клиент вычисляет MD5 (base64) до загрузки. Если файл с таким хешем уже есть в системе, сервер сразу возвращает готовый документ без повторной загрузки.

**Сервисы:**
- `DocumentServiceImpl` — бизнес-логика управления документами.
- `MinIoServiceImpl` — реализует `FileStoragePort`, работа с MinIO: presigned upload/download URL, получение файла.
- `KafkaEventPublisher` — `@TransactionalEventListener(AFTER_COMMIT)`: после коммита транзакции публикует `UploadFileEvent` в Kafka-топик `document-parse-tasks`.
- `KafkaSender` — тонкая обёртка над `KafkaTemplate` с логированием результата.

### rag-core

Ядро RAG-системы: индексация документов, хранение, поиск и чат.

**Индексация (pipeline после парсинга Python-воркером):**
1. `KafkaEventListener` подписывается на топик `json-outgoing`.
2. Получает `DocumentProcessedEvent` (document_id, result_bucket, result_file, status).
3. `DocumentIndexingListener.handleSuccessfulProcessing` скачивает JSON из MinIO, десериализует в `Tree`.
4. `DocumentSectionParserServiceImpl` обходит дерево в глубину (DFS), строит список `DocumentSection` с `ltree`-путями и сохраняет в БД.
5. `DocumentIndexingServiceImpl` разбивает каждую секцию на `Chunk`-и (LangChain4j `DocumentSplitters.recursive`, 1000 символов, overlap 200), батчем генерирует эмбеддинги и сохраняет.

**Поиск (Retrieval):**
- `RetrievalServiceImpl` реализует паттерн **Parent Document Retrieval**:
  1. Генерирует эмбеддинг запроса.
  2. Находит top-K чанков по косинусному сходству через `pgvector` (`<=>` оператор).
  3. Фильтрует по `similarityThreshold` (по умолчанию 0.7).
  4. Батч-загружает родительские секции.
  5. Если секция небольшая (`maxSectionLengthForFullContext`, по умолчанию 2000 символов) — берёт секцию целиком, иначе — чанк с заголовком.

**RAG pipeline** (`RagServiceImpl`):
1. Retrieval — получение релевантных фрагментов.
2. Сборка контекста (с ограничением по `maxContextLength`).
3. Построение промпта (контекст + история диалога + вопрос).
4. Вызов LLM (`LlmClient`).
5. Возврат ответа со списком источников (`SourceReference`).

**Диалоговая сессия:**
- `ChatSessionServiceImpl` хранит `ConversationContext` в Redis с TTL 24 часа.
- `ConversationContext` ограничивает историю диалога по количеству сообщений (`maxHistoryMessages`) и токенам (`maxHistoryTokens`), форматирует её для промпта.
- Ключ сессии: `chat:session:{userId}:{fileAssetId}`.

**Логирование запросов:**
- `QueryLogServiceImpl` — асинхронно (`@Async`) сохраняет в таблицу `query_logs` каждый запрос: вопрос, ответ, количество найденных чанков, min/max similarity score, latency. Предназначено для оценки качества (RAG evaluation).

**REST API:**

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/chat/ask` | Задать вопрос по документу (с учётом истории диалога) |

### app

Точка входа приложения. Содержит:
- `RagApplication` — главный класс `@SpringBootApplication`.
- `SecurityConfig` — OAuth2 Resource Server, интеграция с Keycloak (realm `rag-realm`), stateless сессии, JWT.
- `KafkaConfig` — ручная настройка Producer и Consumer Factory с Jackson-сериализацией.
- `JpaConfig`, `WebConfig` — вспомогательные конфигурации.
- `application.yml` — вся конфигурация приложения.

### doc-parser-python (инфраструктурный воркер)

Асинхронный Python-сервис (asyncio) для разбора PDF-документов.

- Слушает Kafka-топик `document-parse-tasks`.
- Скачивает PDF из MinIO.
- Конвертирует в Markdown постранично (PyMuPDF + pymupdf4llm) с очисткой артефактов (номера страниц, лишние переносы, unicode-мусор).
- `MarkdownTreeBuilder` — строит иерархическое дерево секций по заголовкам (H1-H6).
- Сохраняет JSON-дерево в MinIO (бакет `processed-json-doc`).
- Публикует событие в Kafka-топик `json-outgoing`.
- Реализована idempotency: повторная обработка одного документа пропускается.
- Поддерживает graceful shutdown (SIGINT/SIGTERM).

---

## Основные сущности

### FileAsset

Уникальный физический файл в системе. Дедупликация по MD5-хешу.

| Поле | Тип | Описание |
|---|---|---|
| id | UUID | Первичный ключ |
| hash | VARCHAR | MD5 (base64), уникальный |
| minioBucket | VARCHAR | Бакет MinIO |
| minioPath | VARCHAR | Путь к объекту |
| fileSize | BIGINT | Размер файла |
| contentType | VARCHAR | MIME-тип |
| status | DocumentStatus | Статус обработки |
| failureReason | TEXT | Причина ошибки (если FAILED) |
| processedAt | TIMESTAMP | Время завершения обработки |

**Статусная машина DocumentStatus:** `UPLOADED` → `SENT_TO_PARSER` → `PARSING` → `PARSED` → `INDEXING` → `READY` (или `FAILED` на любом этапе).

### Document

Документ конкретного пользователя. Один `FileAsset` может быть привязан к нескольким `Document` разных пользователей (дедупликация файлов).

| Поле | Тип | Описание |
|---|---|---|
| id | UUID | Первичный ключ |
| filename | VARCHAR | Имя файла (как назвал пользователь) |
| uploadedBy | UUID | ID пользователя (из Keycloak JWT) |
| fileAsset | FK | Ссылка на физический файл |

### DocumentSection

Структурная единица документа — раздел с заголовком. Хранит иерархию секций через `ltree`-путь и self-reference (`parentSection`).

| Поле | Тип | Описание |
|---|---|---|
| id | UUID | Первичный ключ |
| fileAssetId | UUID | Ссылка на FileAsset |
| parentSection | FK (self) | Родительская секция |
| headingTitle | VARCHAR | Заголовок раздела |
| headingDepth | INT | Уровень заголовка (1-6) |
| content | TEXT | Текстовое содержимое |
| sequenceNumber | INT | Порядковый номер среди братьев |
| path | ltree | Путь в дереве (например, `1.2.3`) |

### Chunk

Фрагмент секции для векторного поиска.

| Поле | Тип | Описание |
|---|---|---|
| id | UUID | Первичный ключ |
| content | TEXT | Текст фрагмента |
| chunkIndex | INT | Порядковый номер чанка внутри секции |
| embedding | VECTOR(1536) | Векторное представление (pgvector) |
| documentSection | FK | Родительская секция |

### OutboxEvent

Таблица для паттерна Transactional Outbox (реализован частично, в коде закомментирован шедулер-уборщик).

### QueryLog

Лог каждого RAG-запроса для оценки качества системы.

| Поле | Описание |
|---|---|
| fileAssetId | Документ, по которому задан вопрос |
| userId | Пользователь |
| query | Вопрос |
| answer | Ответ LLM |
| retrievedChunksCount | Количество найденных чанков |
| maxSimilarityScore | Максимальный cosine similarity |
| minSimilarityScore | Минимальный cosine similarity |
| latencyMs | Время выполнения запроса |

---

## Технологический стек

### Backend (Java)
- **Spring Boot** — основной фреймворк
- **Spring Data JPA + Hibernate** — ORM
- **Spring Security (OAuth2 Resource Server)** — аутентификация через JWT/Keycloak
- **Spring Kafka** — интеграция с Kafka
- **Spring Data Redis** — хранение сессий диалога
- **Flyway** — версионирование схемы БД
- **LangChain4j** — разбиение текста на чанки (`DocumentSplitters`)
- **Apache Tika** — извлечение текста из файлов различных форматов
- **MapStruct** — маппинг между Entity и DTO
- **Lombok** — устранение boilerplate
- **Hypersistence Utils** — поддержка `ltree` и других нестандартных типов PostgreSQL

### Базы данных и хранилища
- **PostgreSQL 16 + pgvector** — реляционные данные и векторный поиск
- **Redis** — хранение истории диалогов (TTL 24 часа)
- **MinIO** — S3-совместимое хранилище файлов и результатов парсинга

### Инфраструктура
- **Redpanda** (Kafka-compatible) — брокер сообщений
- **Keycloak** — Identity Provider (OAuth2/OIDC)
- **Docker Compose** — оркестрация локального окружения

### Observability
- **Grafana Tempo** — распределённая трассировка (OTLP)
- **Grafana Loki** — агрегация логов
- **Prometheus** — метрики
- **Grafana Alloy** — агент сбора телеметрии
- **Micrometer** — инструментирование метрик на стороне Java

### Воркер (Python)
- **asyncio + aiokafka** — асинхронный Kafka consumer/producer
- **PyMuPDF (fitz) + pymupdf4llm** — конвертация PDF в Markdown
- **structlog** — структурированное логирование

---

## Текущий статус реализации

### Реализовано

**Управление документами:**
- Двухэтапная загрузка файлов через presigned URL (MinIO)
- Дедупликация файлов по MD5-хешу (один физический файл — один `FileAsset`)
- Хранение метаданных в PostgreSQL, файлов в MinIO
- Пагинированные запросы, поиск по имени, фильтрация по пользователю
- Скачивание через presigned URL
- Удаление документов с публикацией события

**Pipeline индексации:**
- Python-воркер: PDF → Markdown (постраничный, с очисткой) → иерархическое дерево секций → JSON в MinIO
- Java: чтение результата из MinIO по Kafka-событию, построение `DocumentSection`-дерева (DFS + ltree-пути)
- Разбиение секций на `Chunk`-и (LangChain4j, 1000/200 символов)
- Батчевая генерация эмбеддингов и сохранение в pgvector

**RAG pipeline:**
- Векторный поиск top-K через pgvector (`<=>` cosine)
- Фильтрация по `similarityThreshold`
- Parent Document Retrieval: секция целиком или чанк с заголовком
- Сборка промпта с контекстом и историей диалога
- Управление диалоговой сессией в Redis
- Асинхронное логирование запросов в `QueryLog`

**Безопасность:**
- JWT-аутентификация через Keycloak
- Stateless сессии
- Фильтрация документов по `uploadedBy` (userId из JWT)

**Logging-common:**
- AOP-аннотация `@Loggable` с поддержкой slow-request метрики
- MDC-фильтры для request-трассировки
- Передача MDC в асинхронные потоки

**Инфраструктура:**
- Docker Compose для полного локального стека (PostgreSQL, MinIO, Keycloak, Redpanda, Redis)
- Docker Compose для стека наблюдаемости (Tempo, Loki, Prometheus, Grafana)
- Flyway-миграции схемы БД

### Заглушки (требуют реализации)

- `EmbeddingClientImpl` — возвращает нулевые векторы `[0.0 x 1536]`. Интерфейс и инфраструктура готовы, необходимо подключить реальную модель (OpenAI API, локальная модель через LangChain4j и т.п.).
- `LlmClientImpl` — возвращает пустую строку. Интерфейс и весь pipeline построения промпта готовы, необходимо подключить LLM.

### Частично реализовано / оставлено на потом

- Transactional Outbox (`OutboxRelayScheduler`) — сущности и таблица созданы, шедулер закомментирован. Текущая реализация отправки в Kafka напрямую из `@TransactionalEventListener` не гарантирует доставку при падении брокера.
- Обновление `DocumentStatus` в `FileAsset` — поле и enum реализованы, но вызовы `updateStatus` в pipeline закомментированы/не подключены.
- Гибридный поиск (Dense + Sparse через Elasticsearch + RRF) — подробно задокументирован в `docs/hybrid-search.md`, не реализован. Конфигурация Elasticsearch присутствует в `application.yml` (`rag.elasticsearch`), флаг `reranking-enabled=false`.

---

## Дорожная карта

### Этап 1 — Минимально работающий RAG (текущий приоритет)

1. Подключить реальную модель эмбеддингов к `EmbeddingClientImpl`.
   - Варианты: OpenAI `text-embedding-3-small` (1536d), локальная модель через LangChain4j, self-hosted BGE/E5.
   
2. Подключить LLM к `LlmClientImpl`.
   - Варианты: OpenAI GPT-4o, Anthropic Claude, локальная модель через Ollama.

3. Завершить обновление `DocumentStatus` на каждом этапе pipeline (SENT_TO_PARSER → PARSING → PARSED → INDEXING → READY / FAILED).

4. Реализовать `OutboxRelayScheduler` (шедулер-уборщик PENDING-событий) для надёжной доставки в Kafka.

### Этап 2 — Качество и наблюдаемость

5. Гибридный поиск: подключить Elasticsearch/OpenSearch для BM25, реализовать RRF-слияние результатов.

6. Reranker: добавить cross-encoder reranking поверх RRF для повышения точности (флаг `reranking-enabled` уже есть).

7. Кэширование эмбеддингов: избежать повторных вычислений для одинаковых текстов.

8. Подключить Spring Boot приложение к стеку observability (Tempo, Loki, Prometheus): добавить OTEL-агент или Micrometer Tracing.

9. Реализовать endpoint для оценки качества RAG: просмотр `QueryLog`, статистика по similarity scores.

### Этап 3 — Устойчивость и тестирование

10. Добавить retry и circuit breaker для вызовов внешних API (эмбеддинги, LLM).

11. Написать unit-тесты для сервисного слоя (RagServiceImpl, RetrievalServiceImpl, DocumentSectionParserServiceImpl).

12. Написать integration-тесты с Testcontainers (PostgreSQL + pgvector, Redis, MinIO).

13. Добавить Dead Letter Queue для Kafka consumer при ошибках десериализации/обработки.

### Этап 4 — Production

14. OpenAPI/Swagger спецификация для всех endpoint-ов.

15. Rate limiting и защита от злоупотреблений (bucket per user).

16. Kubernetes-манифесты или Helm chart.

17. CI/CD pipeline (GitHub Actions / GitLab CI): сборка, тесты, Docker-образ, деплой.

18. Стратегии повторной индексации: при обновлении документа — атомарная замена секций и чанков.

---

## Запуск локального окружения

### Требования

- Docker Desktop
- Java 21
- Python 3.11+

### 1. Запустить инфраструктуру

```bash
# Основной стек (PostgreSQL, MinIO, Keycloak, Redpanda, Redis)
docker compose -f infrastructure/docker-compose.yml up -d

# Стек наблюдаемости (Tempo, Loki, Prometheus, Grafana) — опционально
docker compose -f infrastructure/docker/docker-compose.observability.yml up -d
```

### 2. Настроить Keycloak

После запуска открыть `http://localhost:8081`, войти под `admin/admin` и:
- Создать realm `rag-realm`.
- Создать клиента для приложения.

### 3. Запустить Python-воркер

```bash
cd infrastructure/doc-parser-python
pip install -r requirements.txt
python -m src.main
```

### 4. Собрать и запустить Java-сервис

```bash
cd backend
./gradlew :core:rag-service:app:bootRun
```

Приложение запустится на `http://localhost:8080`.

### Endpoint-ы (требуется JWT в заголовке Authorization)

```
GET    http://localhost:8080/api/documents
POST   http://localhost:8080/api/documents/init-upload
POST   http://localhost:8080/api/documents/confirm-upload
GET    http://localhost:8080/api/documents/{id}
GET    http://localhost:8080/api/documents/{id}/download
GET    http://localhost:8080/api/documents/search?filename=
DELETE http://localhost:8080/api/documents/{id}
POST   http://localhost:8080/api/chat/ask

GET    http://localhost:8080/actuator/health
GET    http://localhost:8080/v3/api-docs
GET    http://localhost:8080/swagger-ui/index.html
```

---

## Конфигурация (application.yml)

| Параметр | По умолчанию | Описание |
|---|---|---|
| `server.port` | 8080 | Порт приложения |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/rag_db` | БД |
| `spring.data.redis.host` | localhost:6379 | Redis |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka/Redpanda |
| `minio.url` | http://localhost:9000 | MinIO |
| `minio.bucket-name` | rag-documents | Бакет для загрузок |
| `rag.retrieval.default-top-k` | 5 | Количество чанков для retrieval |
| `rag.retrieval.similarity-threshold` | 0.7 | Минимальный cosine score |
| `rag.retrieval.max-section-length-for-full-context` | 2000 | Порог для Parent Doc Retrieval |
| `rag.retrieval.rrf-k` | 60 | Константа RRF (для гибридного поиска) |
| `rag.llm.max-context-length` | 8000 | Максимальный контекст для LLM (символов) |
| `rag.conversation.max-history-messages` | 10 | Лимит истории диалога (сообщений) |
| `rag.conversation.max-history-tokens` | 3000 | Лимит истории диалога (токенов) |
