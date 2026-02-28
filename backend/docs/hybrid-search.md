# Гибридный поиск: Dense + Sparse (Hybrid Search)

## Проблема чистого векторного поиска

`pgvector` отлично находит семантически похожие фрагменты, но **плохо справляется с точными совпадениями**:

| Тип запроса | pgvector (Dense) | BM25 / Elasticsearch (Sparse) |
|---|---|---|
| «объясни принцип форс-мажора» | ✅ хорошо | ❌ плохо |
| «ст. 14.1 КоАП» | ❌ плохо | ✅ хорошо |
| «ГОСТ Р 53692-2009» | ❌ плохо | ✅ хорошо |
| «штрафные санкции за просрочку» | ✅ хорошо | ⚠️ нестабильно |
| «п.5.3.2» | ❌ очень плохо | ✅ хорошо |

**Гибридный поиск** объединяет оба подхода через **Reciprocal Rank Fusion (RRF)**.

---

## Архитектура гибридного поиска

```
Запрос пользователя
        │
   ┌────┴────┐
   │         │
   ▼         ▼
pgvector   Elasticsearch
(Dense)    (Sparse / BM25)
   │         │
   │  top-K  │  top-K
   │ чанков  │ чанков
   └────┬────┘
        │
        ▼
  Reciprocal Rank
     Fusion (RRF)
        │
        ▼
  Объединённый
  ranked список
        │
        ▼
  [опционально]
   Reranker (Cross-Encoder)
        │
        ▼
   Top-N → LLM
```

---

## Reciprocal Rank Fusion (RRF)

Идея: чем выше позиция документа в каждом списке, тем больше его итоговый score.
Формула: $RRF(d) = \sum_{r \in R} \frac{1}{k + \text{rank}_r(d)}$, где $k = 60$ (константа сглаживания).

### Пример: почему RRF лучше простого усреднения scores

```
Документ A: pgvector rank=1 (score 0.95), ES rank=15 → RRF = 1/61 + 1/75 = 0.030
Документ B: pgvector rank=3 (score 0.87), ES rank=2  → RRF = 1/63 + 1/62 = 0.032
```
Документ B побеждает, хотя у A выше vector score — потому что B хорош в обоих поисках.

---

## Реализация

### 1. Зависимости

```kotlin
// build.gradle.kts
dependencies {
    implementation("co.elastic.clients:elasticsearch-java:8.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

### 2. Elasticsearch индексация чанков

```java
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexingService {

    private final ElasticsearchClient esClient;

    // Конфиг — не хардкод:
    @Value("${rag.elasticsearch.index-name:rag-chunks}")
    private String indexName;

    public void indexChunk(UUID chunkId, UUID fileAssetId, String content, String headingTitle) {
        try {
            esClient.index(i -> i
                .index(indexName)
                .id(chunkId.toString())
                .document(Map.of(
                    "chunkId",      chunkId.toString(),
                    "fileAssetId",  fileAssetId.toString(),
                    "content",      content,
                    "headingTitle", headingTitle != null ? headingTitle : ""
                ))
            );
        } catch (Exception e) {
            log.error("Ошибка индексации чанка {} в Elasticsearch: {}", chunkId, e.getMessage(), e);
            throw new RuntimeException("Не удалось проиндексировать чанк в ES", e);
        }
    }
}
```

### 3. BM25 поиск через Elasticsearch

```java
@Service
@RequiredArgsConstructor
public class ElasticsearchRetrievalService {

    private final ElasticsearchClient esClient;
    private final RagProperties ragProperties; // лимиты из application.yml

    @Value("${rag.elasticsearch.index-name:rag-chunks}")
    private String indexName;

    /**
     * Возвращает Map<chunkId → rank> — ранжированный список результатов.
     */
    public Map<UUID, Integer> searchBM25(String query, UUID fileAssetId, int topK) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                .index(indexName)
                .query(q -> q
                    .bool(b -> b
                        .filter(f -> f.term(t -> t
                            .field("fileAssetId")
                            .value(fileAssetId.toString())
                        ))
                        .must(m -> m.multiMatch(mm -> mm
                            .query(query)
                            .fields("content^1.0", "headingTitle^2.0") // заголовок важнее
                        ))
                    )
                )
                .size(topK),
                Map.class
            );

            Map<UUID, Integer> ranks = new LinkedHashMap<>();
            List<Hit<Map>> hits = response.hits().hits();
            for (int i = 0; i < hits.size(); i++) {
                ranks.put(UUID.fromString(hits.get(i).id()), i + 1);
            }
            return ranks;

        } catch (Exception e) {
            log.error("Ошибка BM25 поиска в ES для fileAssetId={}: {}", fileAssetId, e.getMessage(), e);
            return Map.of(); // при ошибке ES — деградируем только на vector search
        }
    }
}
```

### 4. Reciprocal Rank Fusion

```java
@Component
public class ReciprocatRankFusion {

    // Константа сглаживания RRF (стандартно 60)
    @Value("${rag.retrieval.rrf-k:60}")
    private int rrfK;

    /**
     * Объединяет два ranked списка через RRF.
     *
     * @param denseRanks  Map<chunkId → rank> от pgvector
     * @param sparseRanks Map<chunkId → rank> от Elasticsearch
     * @return отсортированный список chunkId по убыванию RRF score
     */
    public List<UUID> fuse(Map<UUID, Integer> denseRanks, Map<UUID, Integer> sparseRanks) {
        Map<UUID, Double> rrfScores = new HashMap<>();

        // Вклад dense поиска
        denseRanks.forEach((id, rank) ->
            rrfScores.merge(id, 1.0 / (rrfK + rank), Double::sum)
        );

        // Вклад sparse поиска
        sparseRanks.forEach((id, rank) ->
            rrfScores.merge(id, 1.0 / (rrfK + rank), Double::sum)
        );

        // Сортируем по убыванию итогового score
        return rrfScores.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();
    }
}
```

### 5. HybridRetrievalService — объединяет всё

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetrievalServiceImpl implements RetrievalService {

    private final ChunkRepository chunkRepository;
    private final DocumentSectionRepository documentSectionRepository;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchRetrievalService esRetrievalService;
    private final ReciprocatRankFusion rrfFusion;
    private final RagProperties ragProperties;

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieve(String query, UUID fileAssetId, int topK) {
        log.debug("Hybrid retrieval: query='{}', fileAssetId={}, topK={}", query, fileAssetId, topK);

        // Шаг 1: Dense поиск (pgvector)
        String embedding = toVectorString(embeddingClient.generateEmbedding(query));
        List<ChunkSimilarityView> denseResults = chunkRepository
            .findTopKByCosineSimilarity(embedding, fileAssetId, topK);

        Map<UUID, Integer> denseRanks = new LinkedHashMap<>();
        for (int i = 0; i < denseResults.size(); i++) {
            denseRanks.put(denseResults.get(i).getId(), i + 1);
        }

        // Шаг 2: Sparse поиск (Elasticsearch BM25)
        Map<UUID, Integer> sparseRanks = esRetrievalService.searchBM25(query, fileAssetId, topK);

        if (sparseRanks.isEmpty()) {
            log.warn("ES BM25 вернул 0 результатов — используем только dense поиск");
        }

        // Шаг 3: RRF fusion
        List<UUID> fusedChunkIds = rrfFusion.fuse(denseRanks, sparseRanks);

        // Берём топ-K после fusion
        List<UUID> topFusedIds = fusedChunkIds.stream()
            .limit(topK)
            .toList();

        // Шаг 4: Загружаем чанки и секции батчем
        Map<UUID, ChunkSimilarityView> denseById = denseResults.stream()
            .collect(Collectors.toMap(ChunkSimilarityView::getId, Function.identity()));

        Set<UUID> sectionIds = topFusedIds.stream()
            .map(id -> denseById.containsKey(id)
                ? denseById.get(id).getDocumentSectionId()
                : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<UUID, DocumentSection> sectionsById = documentSectionRepository
            .findAllById(sectionIds).stream()
            .collect(Collectors.toMap(DocumentSection::getId, Function.identity()));

        // Шаг 5: Сборка результатов
        return topFusedIds.stream()
            .filter(denseById::containsKey) // только те, что есть в dense (у них есть score)
            .map(id -> {
                ChunkSimilarityView view = denseById.get(id);
                DocumentSection section = sectionsById.get(view.getDocumentSectionId());
                if (section == null) return null;

                String contextText = resolveContextText(view, section,
                    ragProperties.getRetrieval().getMaxSectionLengthForFullContext());

                return RetrievedChunk.builder()
                    .chunkId(view.getId())
                    .sectionId(section.getId())
                    .headingTitle(section.getHeadingTitle())
                    .contextText(contextText)
                    .similarityScore(view.getSimilarityScore())
                    .pageNumber(section.getSequenceNumber())
                    .build();
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private String resolveContextText(ChunkSimilarityView chunk, DocumentSection section, int maxLen) {
        if (section.getContent() != null && section.getContent().length() < maxLen) {
            String h = section.getHeadingTitle() != null ? section.getHeadingTitle() : "";
            return h.isBlank() ? section.getContent() : h + "\n\n" + section.getContent();
        }
        String h = section.getHeadingTitle() != null ? section.getHeadingTitle() : "";
        return h.isBlank() ? chunk.getContent() : h + "\n\n" + chunk.getContent();
    }

    private String toVectorString(List<Double> embedding) {
        return embedding.stream()
            .map(Object::toString)
            .collect(Collectors.joining(",", "[", "]"));
    }
}
```

---

## Конфигурация в application.yml

```yaml
rag:
  elasticsearch:
    index-name: rag-chunks
    host: localhost
    port: 9200
  retrieval:
    rrf-k: 60                         # константа сглаживания RRF
    default-top-k: 5                  # сколько чанков брать из каждого поиска
    similarity-threshold: 0.0         # минимальный pgvector score
    max-section-length-for-full-context: 2000
```

---

## Деградация при недоступности ES

Если Elasticsearch недоступен, `ElasticsearchRetrievalService.searchBM25()` возвращает
пустой `Map.of()`. В этом случае `HybridRetrievalServiceImpl` автоматически использует
только dense результаты — система продолжит работать без BM25.

```
ES недоступен → sparseRanks = {} → RRF работает только на denseRanks → деградация, не сбой
```

---

## Reranking (следующий шаг)

После RRF fusion можно добавить Cross-Encoder reranker для точного переранжирования:

```java
// Псевдокод
List<RetrievedChunk> rerankIfNeeded(String query, List<RetrievedChunk> candidates) {
    if (!ragProperties.getRetrieval().isRerankingEnabled()) {
        return candidates; // пропускаем, если не включено
    }
    // Cross-Encoder score: насколько (query, chunk_text) релевантны
    return rerankingClient.rerank(query, candidates);
}
```

Варианты:
- **Cohere Rerank API** — hosted решение
- **BGE-Reranker** — open source, self-hosted
- **LLM-as-judge** — дорого, но точно
