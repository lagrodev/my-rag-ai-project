# Roadmap — RAG AI Project

> Дата: март 2026

---

## Текущее состояние

| Компонент | Статус |
|---|---|
| Document upload flow (init → confirm → Kafka → Python → Kafka → indexing) | ✅ работает |
| `DocumentStatus` enum (UPLOADED → … → READY / FAILED) | ✅ определён, ❌ не подключён |
| `OutboxRelayScheduler` | ❌ закомментирован |
| `EmbeddingClientImpl` | ❌ заглушка (возвращает нули) |
| `LlmClientImpl` | ❌ заглушка (возвращает пустую строку) |
| Hybrid search (pgvector + BM25 + RRF) | 🗒️ задокументирован, не реализован |
| Reranking | ❌ флаг есть, логики нет |
| Observability (Grafana, Tempo, Loki) | ✅ инфра готова |

---

## Фаза 1 — Статусы документов + Real-time уведомления

> **Цель:** пользователь видит, на каком этапе находится обработка его документа — в реальном времени.

---

### 1.1 Прокинуть `DocumentStatus` по всему пайплайну

#### Целевой граф переходов

```
UPLOADED → SENT_TO_PARSER → PARSING → PARSED → INDEXING → READY
                                                          └→ FAILED (на любом этапе)
```

`DocumentStatus` enum уже существует в `FileAsset.java`. `FileAsset` уже хранит `status`, `failureReason`, `processedAt`. Нужно только прокинуть вызовы.

---

#### Шаг 1 — `UPLOADED` (уже частично работает)

**Файл:** `DocumentServiceImpl.confirmDocumentUpload()`

`FileAsset` создаётся с `@Builder.Default status = UPLOADED`. Статус выставлен корректно — ничего менять не нужно.

---

#### Шаг 2 — `SENT_TO_PARSER`

**Файл:** `KafkaEventPublisher.handleUploadFileEvent()`

После успешной отправки в Kafka нужно обновить статус `FileAsset`. Сейчас этого нет — есть только `log.error` при ошибке.

```java
// KafkaEventPublisher.java — добавить после успешной отправки:
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleUploadFileEvent(UploadFileEvent event) {
    try {
        kafkaSender.send("document-parse-tasks", event.id().toString(), event);
        fileAssetRepository.updateStatus(event.id(), DocumentStatus.SENT_TO_PARSER); // добавить
    } catch (Exception e) {
        log.error("Ошибка отправки в Kafka для asset: {}", event.id(), e);
        fileAssetRepository.updateStatusWithReason(event.id(), DocumentStatus.FAILED, e.getMessage()); // добавить
    }
}
```

> ⚠️ `fileAssetRepository.updateStatus(id, status)` — добавить в `FileAssetRepository`:
> ```java
> @Modifying
> @Query("UPDATE FileAsset f SET f.status = :status WHERE f.id = :id")
> void updateStatus(@Param("id") UUID id, @Param("status") DocumentStatus status);
> ```

---

#### Шаг 3 — `PARSING` и `PARSED` (Python → Java)

**Проблема:** сейчас Python публикует в `json-outgoing` только финальный результат со `status: "processed"`. Java ничего не знает о том, что Python вообще начал работу.

---

##### 🔀 Архитектурный выбор: Kafka vs gRPC для статус-апдейтов

Два варианта доставки промежуточных статусов из Python в Java:

| Критерий | Kafka (`document-status-updates`) | gRPC streaming |
|---|---|---|
| **Направление** | Python → Java (push) | Java → Python (pull/subscribe) |
| **Семантика** | fire-and-forget, async | request-response или server-stream |
| **Надёжность** | At-Least-Once, персистентный лог, replay | соединение упадёт — стрим прервётся |
| **Связность** | слабая — Python не знает о Java | сильная — Java держит открытое соединение к Python |
| **Сложность добавления** | низкая — уже есть aiokafka + KafkaTemplate | высокая — proto-файлы, кодогенерация, grpcio-aio |
| **Когда Java оффлайн** | сообщения лежат в топике, будут доставлены при старте | соединение недоступно, статус потерян |
| **Подходит для автообновления SSE** | ✅ Kafka-listener напрямую триггерит SSE-push | ✅ тоже работает, но сложнее хранить стрим |
| **On-demand запрос статуса** | ❌ нельзя — только подписка | ✅ можно дёрнуть `GetParseStatus()` в любой момент |

**Вывод — для статус-апдейтов правильный выбор это Kafka:**

1. **Python уже является продюсером**, добавить один топик — минимальные изменения
2. **Надёжность важнее**: если rag-service рестартнул во время парсинга — оффсет в Kafka сохранён, статус придёт при восстановлении
3. **Автообновление SSE** нативно ложится: `KafkaListener` → `SseService.push()` — прямая цепочка без дополнительного слоя
4. **gRPC имеет смысл добавить позже** (Фаза 2) для on-demand запроса: "а какой сейчас статус вот этого документа?" — тогда Java дёргает Python синхронно при HTTP-запросе фронта, не ожидая Kafka-события

**Итог:** статус-апдейты идут через Kafka (push-модель). gRPC — отдельная фича для синхронного опроса on-demand.

---

**Решение для этого шага:** добавить новый топик `document-status-updates`.

##### Python — `service.py`
Добавить вызовы `status_producer.send_status(...)` в `DocumentProcessingService.process_event()`:

```python
# Шаг 1 — сразу после получения задачи (до download)
await status_producer.send_status(event.id, "PARSING", "Начало парсинга PDF")

# Шаг 2 — после upload_json (парсинг и сохранение успешны)
await status_producer.send_status(event.id, "PARSED", "Документ распарсен и сохранён")

# При любой ошибке — в except:
await status_producer.send_status(event.id, "FAILED", str(e))
```

##### Python — новый `status_producer.py`
```python
# infrastructure/kafka/status_producer.py
class DocumentStatusProducer:
    async def send_status(self, document_id: str, status: str, message: str):
        payload = {
            "document_id": document_id,
            "status": status,
            "message": message,
            "timestamp": datetime.utcnow().isoformat()
        }
        await self.producer.send_and_wait(
            "document-status-updates",
            key=document_id.encode(),
            value=json.dumps(payload).encode()
        )
```

##### Java — новый `DocumentStatusUpdateEvent` record
```java
// features/rag-core — новый record:
record DocumentStatusUpdateEvent(
    @JsonProperty("document_id") UUID documentId,
    String status,
    String message,
    Instant timestamp
) {}
```

##### Java — новый `@KafkaListener` в `KafkaEventListener`
```java
@KafkaListener(topics = "document-status-updates", groupId = "chat-ai-group")
public void handleDocumentStatusUpdate(String rawJson) {
    DocumentStatusUpdateEvent event = objectMapper.readValue(rawJson, DocumentStatusUpdateEvent.class);
    DocumentStatus newStatus = DocumentStatus.valueOf(event.status().toUpperCase());
    
    if (newStatus == DocumentStatus.FAILED) {
        fileAssetRepository.updateStatusWithReason(event.documentId(), newStatus, event.message());
    } else {
        fileAssetRepository.updateStatus(event.documentId(), newStatus);
    }
    
    // 👇 пуш в SSE (Шаг 1.2)
    sseService.push(event.documentId(), newStatus, event.message());
}
```

---

#### Шаг 4 — `INDEXING` и `READY` / `FAILED`

**Файл:** `DocumentIndexingListener.handleSuccessfulProcessing()` и `DocumentIndexingServiceImpl.indexDocumentForSections()`

```java
// DocumentIndexingListener — в начале метода:
fileAssetRepository.updateStatus(documentId, DocumentStatus.INDEXING);
sseService.push(documentId, DocumentStatus.INDEXING, "Индексирование документа...");

try {
    // ... существующий код парсинга секций и индексирования ...
    documentIndexingService.indexDocumentForSections(sections);
    
    fileAssetRepository.updateStatus(documentId, DocumentStatus.READY);
    fileAssetRepository.updateProcessedAt(documentId, LocalDateTime.now());
    sseService.push(documentId, DocumentStatus.READY, "Документ готов к поиску");

} catch (Exception e) {
    fileAssetRepository.updateStatusWithReason(documentId, DocumentStatus.FAILED, e.getMessage());
    sseService.push(documentId, DocumentStatus.FAILED, "Ошибка индексирования: " + e.getMessage());
    throw e;
}
```

---

#### Итоговая карта изменений (шаг 1.1)

| Файл | Что добавить |
|---|---|
| `FileAssetRepository` | `updateStatus()`, `updateStatusWithReason()`, `updateProcessedAt()` |
| `KafkaEventPublisher` | вызов `updateStatus(SENT_TO_PARSER)` / `(FAILED)` |
| `KafkaEventListener` | новый `@KafkaListener("document-status-updates")` |
| `DocumentIndexingListener` | вызов `updateStatus(INDEXING)`, `(READY)`, `(FAILED)` |
| `DocumentStatusUpdateEvent` | новый record (Python → Java schema) |
| Python `service.py` | вызовы `status_producer.send_status(...)` |
| Python `status_producer.py` | новый класс `DocumentStatusProducer` |

---

### 1.0 — Архитектурный вопрос: как связать модули для обновления статуса

> Это фундаментальный вопрос, который нужно решить **перед** реализацией шагов 1.1–1.2.

#### Проблема

Обновление статуса документа происходит в двух разных модулях одного проекта:

```
document-manager              rag-core
─────────────────             ─────────────────
FileAsset (entity)            KafkaEventListener
DocumentStatus (enum)         DocumentIndexingListener
FileAssetRepository           DocumentIndexingServiceImpl
DocumentStatusSseService
AssetStatusListener
```

`rag-core` уже знает, когда документ перешёл в `INDEXING` → `READY` / `FAILED`, но **не имеет доступа** к `FileAssetRepository` и `DocumentStatusSseService` — они живут в `document-manager`, от которого `rag-core` не зависит.

Граф зависимостей сейчас:
```
app → document-manager → core-utils
app → rag-core         → core-utils
```

`rag-core` → `document-manager` — добавлять **нельзя**: это сильная связность, нарушение принципа разделения ответственности, возможный circular dependency в будущем.

---

#### Варианты решения

##### Вариант A — Прямая зависимость `rag-core → document-manager` ❌

```kotlin
// rag-core/build.gradle.kts
implementation(project(":features:document-manager")) // не делать
```

**Плюсы:** быстро, просто.

**Минусы:** `rag-core` знает о MinIO, маппере, DocumentDto и прочих деталях `document-manager`. Любое изменение в `document-manager` может сломать `rag-core`. Nарушение принципа: тот, кто индексирует, не должен знать, как хранится статус.

---

##### Вариант B — Перенести `DocumentStatusEvent` в `core-utils` и использовать Spring Events ✅ (быстрый win)

`core-utils` уже содержит `UploadFileEvent` — именно по этой же логике. Нужно перенести туда `DocumentStatusEvent` и `DocumentStatus`.

```
core-utils (shared)
├── UploadFileEvent          ← уже здесь
├── DocumentStatusEvent      ← перенести из document-manager
└── DocumentStatus           ← перенести из document-manager
```

Тогда:
- `rag-core` публикует через `ApplicationEventPublisher`:
  ```java
  // DocumentIndexingListener (rag-core) — никакого импорта из document-manager
  eventPublisher.publishEvent(new DocumentStatusEvent(fileAssetId, DocumentStatus.INDEXING, "Индексирование...", Instant.now()));
  ```
- `document-manager`'s `AssetStatusListener` ловит и обновляет БД + SSE:
  ```java
  // AssetStatusListener (document-manager) — уже существует, он уже слушает этот тип
  @EventListener
  public void on(DocumentStatusEvent event) {
      fileAssetRepository.updateStatus(event.fileAssetId(), event.status());
      sseService.push(event.fileAssetId(), event.status(), event.message());
  }
  ```

**Плюсы:** минимальные изменения, паттерн уже используется в проекте, `rag-core` не знает ни о репозитории, ни о SSE.

**Минусы:** `core-utils` теперь знает о бизнес-концепте `DocumentStatus`. Для небольшого монолита — приемлемо.

---

##### Вариант C — Port/Adapter через `core-utils` ✅ (чище, рекомендуется)

Вместо конкретного класса события — абстрактный порт:

```java
// core-utils: новый интерфейс — порт
public interface DocumentStatusPort {
    void updateStatus(UUID fileAssetId, DocumentStatus status, String message);
}
```

```java
// document-manager: адаптер реализует порт
@Component
@RequiredArgsConstructor
public class DocumentStatusAdapter implements DocumentStatusPort {
    private final ApplicationEventPublisher publisher;

    @Override
    public void updateStatus(UUID fileAssetId, DocumentStatus status, String message) {
        publisher.publishEvent(new DocumentStatusEvent(fileAssetId, status, message, Instant.now()));
        // DocumentStatusEvent → AssetStatusListener → БД + SSE
    }
}
```

```java
// rag-core: знает только о порте из core-utils
@Component
@RequiredArgsConstructor
public class DocumentIndexingListener {
    private final DocumentStatusPort statusPort; // инжектится адаптер из document-manager

    public void handleSuccessfulProcessing(UUID fileAssetId, ...) {
        statusPort.updateStatus(fileAssetId, DocumentStatus.INDEXING, "Индексирование...");
        try {
            // ... индексирование ...
            statusPort.updateStatus(fileAssetId, DocumentStatus.READY, "Готов");
        } catch (Exception e) {
            statusPort.updateStatus(fileAssetId, DocumentStatus.FAILED, e.getMessage());
        }
    }
}
```

**Плюсы:**
- `rag-core` зависит только от абстракции, не от реализации
- Легко мокировать в тестах (`Mockito.mock(DocumentStatusPort.class)`)
- Реализацию можно менять (сейчас Spring Events, потом — прямой вызов, Kafka, что угодно) без изменений в `rag-core`
- Явный контракт: "кто-то умеет обновлять статус, мне не важно как"

**Минусы:** чуть больше кода (2 новых файла vs 1 перемещённый).

---

##### Вариант D — Port/Adapter + Kafka с первого дня ✅✅ (рекомендуется при планах на микросервисы)

Это вариант C, но адаптер реализован через Kafka, а не через Spring Events.

Ключевое наблюдение: **Spring ApplicationEvents работают только внутри одного JVM процесса**. При разделении на микросервисы:
- Вариант B полностью ломается — не работает across JVM
- Вариант C (Spring Events адаптер) — ломается в адаптере, нужно переписывать
- Вариант D — **не ломается ничего**, меняется только топология деплоя

```java
// Адаптер в rag-core (адаптер живёт там же, где продюсер)
@Component
@RequiredArgsConstructor
public class DocumentStatusKafkaAdapter implements DocumentStatusPort {
    private final KafkaSender kafkaSender;

    @Override
    public void updateStatus(UUID fileAssetId, DocumentStatus status, String message) {
        kafkaSender.send(
            "document-status-updates",
            fileAssetId.toString(),
            new DocumentStatusUpdatePayload(fileAssetId, status.name(), message, Instant.now())
        );
    }
}
```

```java
// document-manager: слушает document-status-updates (новый @KafkaListener)
// KafkaEventListener (или отдельный класс в document-manager)
@KafkaListener(topics = "document-status-updates", groupId = "chat-ai-group")
public void handleStatusUpdate(String rawJson) {
    DocumentStatusUpdatePayload event = objectMapper.readValue(rawJson, ...);
    DocumentStatus status = DocumentStatus.valueOf(event.status());
    fileAssetRepository.updateStatus(event.fileAssetId(), status, event.message());
    sseService.push(event.fileAssetId(), status, event.message());
}
```

**Что происходит при миграции на микросервисы:**

```
Сейчас (монолит):                      После разделения (микросервисы):

indexing-module                          indexing-service
  │ Kafka: document-status-updates  →      │ Kafka: document-status-updates  →
  ▼                                        ▼
document-module (same JVM)              document-service (other JVM/pod)
  KafkaListener → БД + SSE                KafkaListener → БД + SSE
```

**Изменения при миграции = ноль строк бизнес-кода.** Только `docker-compose` / `k8s` manifests.

---

#### Рекомендация

|                                    | A: прямая зависимость | B: Spring Events в core-utils | C: Port + Spring Events | D: Port + Kafka |
| ---------------------------------- | :-------------------: | :---------------------------: | :---------------------: | :-------------: |
| Сложность реализации               |        низкая         |            низкая             |         средняя         |     средняя     |
| Связность модулей                  |       высокая ❌       |           низкая ✅            |        низкая ✅         |    низкая ✅     |
| Testability                        |       плохая ❌        |            средняя            |       отличная ✅        |   отличная ✅    |
| Выживет в микросервисах            |         нет ❌         |             нет ❌             |    нет (адаптер) ⚠️     |      да ✅       |
| Стоимость миграции на микросервисы |    полный рефактор    |        полный рефактор        |   переписать адаптер    |      ноль       |

**→ Вариант D** — если рассматривается миграция на микросервисы.
**→ Вариант C** — если монолит навсегда, хочется просто чисто.

Вариант A, B — не делать.

> ⚠️ Kafka уже есть в инфраструктуре (Redpanda), топик `document-status-updates` уже запланирован для Python → Java статусов. Добавить в него же Java-internal статусы — это **нулевая** инфраструктурная стоимость.

---

#### Итоговая структура (Вариант D)

```
core-utils
├── UploadFileEvent                    ← существует
├── DocumentStatus                     ← перенести из document-manager
└── DocumentStatusPort                 ← новый интерфейс

rag-core
├── adapters/DocumentStatusKafkaAdapter← implements DocumentStatusPort → kafkaSender.send(...)
├── KafkaEventListener                 ← (без изменений, слушает json-outgoing)
└── DocumentIndexingListener           ← инжектит DocumentStatusPort

document-manager
├── listeners/AssetStatusListener      ← @KafkaListener("document-status-updates") → БД + SSE
└── service/DocumentStatusSseService   ← существует
```

Поток данных (монолит и микросервис — идентичный):
```
DocumentIndexingListener
  statusPort.updateStatus(fileAssetId, INDEXING, "...")
      │
      ▼ (Kafka: document-status-updates)
      │
      ▼
AssetStatusListener (document-manager)
  ├── fileAssetRepository.updateStatus()  →  PostgreSQL
  └── sseService.push()                   →  SSE  →  Browser

Python doc-parser
  status_producer.send_status(id, "PARSING", "...")
      │
      ▼ (тот же Kafka топик: document-status-updates)
      │
      ▼
AssetStatusListener (document-manager)  ← единая точка входа для всех статус-апдейтов
  ├── fileAssetRepository.updateStatus()
  └── sseService.push()
```

Бонус: **Python и Java-internal статусы идут в один топик**, `AssetStatusListener` — единая точка входа. Не нужно разводить два пути.

---

### 1.2 SSE — стриминг статусов пользователю

Фронтенд открывает одно долгоживущее HTTP-соединение и получает события по мере прохождения документа через пайплайн. Никакого polling'а.

```http
GET /api/documents/{id}/status/stream
Authorization: Bearer <token>
Accept: text/event-stream
```

#### Технология: Spring MVC `SseEmitter`

Проект использует Spring MVC (не WebFlux) — используем `SseEmitter`.

```java
// Новый класс: features/notifications/DocumentStatusSseService.java

@Service
public class DocumentStatusSseService {

    // documentId → список активных подписчиков
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
        new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID documentId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 минут таймаут
        emitters.computeIfAbsent(documentId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Удалить при завершении / ошибке / таймауте
        Runnable cleanup = () -> removeEmitter(documentId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public void push(UUID documentId, DocumentStatus status, String message) {
        List<SseEmitter> list = emitters.getOrDefault(documentId, new CopyOnWriteArrayList<>());
        DocumentStatusEvent event = new DocumentStatusEvent(documentId, status, message, Instant.now());

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                    .name("document-status")
                    .data(event, MediaType.APPLICATION_JSON));

                // Закрыть стрим на финальных статусах
                if (status == DocumentStatus.READY || status == DocumentStatus.FAILED) {
                    emitter.complete();
                    dead.add(emitter);
                }
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    private void removeEmitter(UUID documentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(documentId);
        if (list != null) list.remove(emitter);
    }
}
```

#### Схема события `DocumentStatusEvent`

```java
record DocumentStatusEvent(
    UUID documentId,
    DocumentStatus status,   // UPLOADED | SENT_TO_PARSER | PARSING | PARSED | INDEXING | READY | FAILED
    String message,          // человекочитаемое описание
    Instant timestamp
) {}
```

Пример того, что получит браузер:

```
event: document-status
data: {"documentId":"...","status":"PARSING","message":"Начало парсинга PDF","timestamp":"2026-03-11T12:00:01Z"}

event: document-status
data: {"documentId":"...","status":"INDEXING","message":"Индексирование документа...","timestamp":"2026-03-11T12:00:05Z"}

event: document-status
data: {"documentId":"...","status":"READY","message":"Документ готов к поиску","timestamp":"2026-03-11T12:00:12Z"}
```

#### `DocumentStatusController`

```java
// Добавить в DocumentController или выделить отдельный:

@GetMapping(value = "/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamDocumentStatus(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt
) {
    UUID userId = UUID.fromString(jwt.getSubject());
    // Проверить, что документ принадлежит userId (доступ)
    documentService.getDocumentMetadata(id, userId); // бросит 404/403 если нет доступа

    // Сразу отправить текущий статус (если пользователь подключился после начала обработки)
    SseEmitter emitter = sseService.subscribe(id);
    DocumentStatus current = fileAssetRepository.findStatusById(id);
    if (current != null) {
        sseService.pushCurrentStatus(id, current); // sendCurrentStatusOnConnect
    }

    return emitter;
}
```

#### Конфигурация Spring — разрешить long-polling

В `WebConfig` добавить поддержку async:

```java
@Override
public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(5 * 60 * 1000L);
}
```

В `SecurityConfig` — разрешить эндпоинт SSE с теми же правилами (JWT уже проверяется через `getDocumentMetadata`).

---

#### Итоговая карта изменений (шаг 1.2)

| Файл                            | Что создать / изменить                              |
| ------------------------------- | --------------------------------------------------- |
| `DocumentStatusEvent.java`      | новый record — DTO для SSE-события                  |
| `DocumentStatusSseService.java` | новый @Service — управление SseEmitter'ами          |
| `DocumentController.java`       | добавить `GET /{id}/status/stream`                  |
| `WebConfig.java`                | настроить async timeout                             |
| `KafkaEventListener`            | вызывать `sseService.push(...)` в обоих listener'ах |
| `DocumentIndexingListener`      | вызывать `sseService.push(...)` при смене статуса   |

---

### 1.3 Восстановить `OutboxRelayScheduler`

`OutboxRelayScheduler` полностью закомментирован. Это критично: если JVM упала между `@TransactionalEventListener(AFTER_COMMIT)` и реальной отправкой в Kafka — событие потеряно навсегда.

**Что нужно:**
1. Раскомментировать `@Component` и логику класса
2. В `KafkaEventPublisher.handleUploadFileEvent()` — при ошибке отправки сохранять `OutboxEvent` со`state = PENDING`
3. `@Scheduled(fixedDelay = 300_000)` сканирует `PENDING` события → пробует переотправить → ставит `PROCESSED` или `FAILED`

До реализации Outbox — хотя бы убрать silent failure: сейчас `log.error` и всё, пользователь никогда не узнает что документ завис.

---

### 1.4 (Опционально) Fallback — polling эндпоинт

Для клиентов, которые не поддерживают SSE (или при обрыве соединения):

```http
GET /api/documents/{id}/status
→ { "status": "INDEXING", "message": "...", "updatedAt": "..." }
```

Этот эндпоинт уже частично реализован через `GET /api/documents/{id}` (возвращает `DocumentDto`). Достаточно убедиться, что `DocumentDto` включает `status` и `failureReason`.

---

## Фаза 2 — gRPC

> **Цель:** синхронные on-demand запросы между сервисами там, где Kafka не подходит (request-response семантика, мгновенный ответ нужен прямо сейчас).

### Почему не для статус-апдейтов?

Статус-апдейты в реальном времени уже решены через Kafka в Фазе 1 (push-модель: Python сам шлёт события). gRPC здесь избыточен.

**Где gRPC нужен:** фронт открыл страницу документа, который обрабатывался пока сервис был оффлайн. Kafka-события уже прошли, SSE-подписки не было. Нужно _прямо сейчас_ узнать актуальный статус у Python-воркера.

### Сценарии применения

| Сценарий | Почему Kafka не подходит | Почему gRPC |
|---|---|---|
| `GET /api/documents/{id}/status` — фронт опрашивает текущий статус | Kafka не отвечает на вопросы, только доставляет события | Синхронный `GetParseStatus()` — ответ за миллисекунды |
| Java хочет знать, сколько задач в очереди у Python | pull-семантика | `GetWorkerStats()` |
| Будущий `embedding-service` | Нужен ответ (вектор) прямо сейчас | Унарный RPC |

### Что нужно сделать

**Protobuf-контракты** (новый модуль `common/grpc-contracts`):

```protobuf
// parse_service.proto
service DocumentParseService {
  // Синхронный запрос текущего статуса документа
  rpc GetParseStatus (ParseStatusRequest) returns (ParseStatusResponse);
}

message ParseStatusRequest {
  string document_id = 1;
}

message ParseStatusResponse {
  string document_id = 1;
  string status  = 2;   // IDLE | QUEUED | PARSING | PARSED | FAILED
  string message = 3;
  int32  progress_pct = 4;
}
```

**Java (gRPC client):**
- Зависимости: `grpc-stub`, `grpc-netty-shaded`, `protobuf-java` + Gradle plugin `com.google.protobuf`
- `ParseServiceGrpcClient` — обёртка над сгенерированным стабом
- Вызывается из `DocumentController.getDocumentStatus()` как fallback, если Kafka-событий ещё не было

**Python (gRPC server):**
- `grpcio`, `grpcio-tools`, `grpcio-aio`
- `ParseServiceServicer` — хранит in-memory `dict[document_id → status]`, обновляет при каждом шаге pipeline
- Запускается рядом с Kafka-consumer'ом в том же asyncio event loop

**Взаимодействие с SSE:**
```
Фронт открыл страницу → GET /api/documents/{id}/status
    → Java: статус в БД == SENT_TO_PARSER — давно, что-то пошло не так?
    → Java делает gRPC-вызов GetParseStatus к Python
    → Python отвечает актуальным состоянием
    → Java обновляет БД, пушит в SSE (если подписка есть)
```

---

## Фаза 3 — Подключить реальные LLM / Embedding

> **Цель:** заменить заглушки на реальные вызовы.

### 3.1 EmbeddingClientImpl
- Вариант A: **OpenAI** `text-embedding-3-small` (1536 dims) — уже совпадает с `VECTOR(1536)` в БД
- Вариант B: **Ollama** локально (`nomic-embed-text`, 768 dims — нужно поменять размерность)

Имплементировать `EmbeddingPort` через `LangChain4j` — библиотека уже в зависимостях (`langchain4j-open-ai 1.11.0`):
```java
OpenAiEmbeddingModel.builder()
    .apiKey(apiKey)
    .modelName("text-embedding-3-small")
    .build();
```

### 3.2 LlmClientImpl
- LangChain4j `OpenAiChatModel` / `OllamaChatModel`
- Streaming ответа через `StreamingChatLanguageModel` → уже можно стримить через SSE (добавить `GET /api/chat/stream`)

---

## Фаза 4 — Hybrid Search

Подробная архитектура уже есть в [hybrid-search.md](hybrid-search.md).

**Порядок реализации:**
1. Добавить `tsvector`-колонку в `document_chunks` + GIN-индекс
2. Реализовать BM25-поиск через `to_tsquery` (без Elasticsearch — pure PostgreSQL)
3. Реализовать RRF-слияние в `ChunkRepository` (или JPQL)
4. Включить флаг `reranking-enabled: true` + реализовать `RerankingService` (cross-encoder через LangChain4j или внешний API)

---

## Фаза 5 — Production Hardening

| Задача | Приоритет |
|---|---|
| Восстановить `OutboxRelayScheduler` (Transactional Outbox pattern) | 🔴 высокий |
| Dead-letter-queue для Kafka (топик `document-parse-tasks.DLT`) | 🟠 средний |
| Исправить расхождение realm Keycloak (`rag-realm` vs `rag`) | 🔴 высокий |
| Springdoc OpenAPI (`/swagger-ui.html`) | 🟡 низкий |
| Integration-тесты (`docker-compose.test.yml` уже есть) | 🟠 средний |
| Rate limiting на `/api/chat/ask` | 🟠 средний |

---

## Порядок выполнения (рекомендуемый)

```
[1] Прокинуть DocumentStatus по пайплайну (1.1)
      ↓
[2] SSE-эндпоинт для статусов (1.2)
      ↓
[3] Подключить реальные Embedding + LLM (3.1, 3.2)
      ↓
[4] gRPC для статус-стриминга Python→Java (2)
      ↓
[5] Hybrid Search (4)
      ↓
[6] Hardening (5)
```

---

## Новые Kafka-топики (план)

| Топик | Направление | Назначение |
|---|---|---|
| `document-parse-tasks` | Java → Python | ✅ существует |
| `json-outgoing` | Python → Java | ✅ существует |
| `document-status-updates` | Python → Java | промежуточные статусы парсинга |

---

## Новые модули (план)

| Модуль | Расположение | Назначение |
|---|---|---|
| `common/grpc-contracts` | `backend/core/rag-service/common/grpc-contracts` | `.proto` файлы + сгенерированные стабы |
| `features/notifications` | `backend/core/rag-service/features/notifications` | SSE-сервис, Kafka→SSE bridge |
