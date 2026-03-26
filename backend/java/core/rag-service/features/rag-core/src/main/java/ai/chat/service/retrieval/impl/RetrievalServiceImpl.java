package ai.chat.service.retrieval.impl;

import ai.chat.config.RagProperties;
import ai.chat.entity.DocumentSection;
import ai.chat.repository.ChunkRepository;
import ai.chat.repository.DocumentSectionRepository;
import ai.chat.repository.projection.ChunkSimilarityView;
import ai.chat.service.embed.EmbeddingClient;
import ai.chat.service.retrieval.RetrievalService;
import ai.chat.service.retrieval.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реализация Retrieval-сервиса по паттерну Parent Document Retrieval:
 * 1. Эмбеддинг запроса
 * 2. Поиск top-K чанков по косинусному сходству (pgvector)
 * 3. Для каждого чанка берём родительскую секцию
 * 4. Выбираем контекст: секция целиком (если небольшая) или чанк с заголовком
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final ChunkRepository chunkRepository;
    private final DocumentSectionRepository documentSectionRepository;
    private final EmbeddingClient embeddingClient;
    private final RagProperties ragProperties;

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieve(String query, UUID fileAssetId, int topK) {
        log.debug("Retrieval запрос: query='{}', fileAssetId={}, topK={}", query, fileAssetId, topK);

        // Эмбеддинг запроса
        String embeddingString;
        try {
            List<Double> queryEmbedding = embeddingClient.generateEmbedding(query); // тут вектора
            embeddingString = toVectorString(queryEmbedding);
        } catch (Exception e) {
            log.error("Ошибка генерации эмбеддинга для запроса '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Не удалось сгенерировать эмбеддинг запроса", e);
        }

        // Топ-K чанков по косинусному сходству
        List<ChunkSimilarityView> topChunks;
        try {
            topChunks = chunkRepository.findTopKByCosineSimilarity(embeddingString, fileAssetId, topK);
        } catch (Exception e) {
            log.error("Ошибка pgvector-поиска для fileAssetId={}: {}", fileAssetId, e.getMessage(), e);
            throw new RuntimeException("Ошибка векторного поиска", e);
        }

        if (topChunks.isEmpty()) {
            log.warn("Не найдено чанков для fileAssetId={} по запросу '{}'", fileAssetId, query);
            return List.of();
        }

        // Фильтрация по порогу сходства
        double threshold = ragProperties.getRetrieval().getSimilarityThreshold(); // по дефолту - 0.7
        List<ChunkSimilarityView> filtered = topChunks.stream()
                .filter(c -> c.getSimilarityScore() != null && c.getSimilarityScore() >= threshold)
                .toList();

        if (filtered.isEmpty()) {
            log.warn("Все {} чанков отфильтрованы по порогу similarityThreshold={} для fileAssetId={}",
                    topChunks.size(), threshold, fileAssetId);
            return List.of();
        }

        //  Батч-загрузка родительских секций
        Set<UUID> sectionIds = filtered.stream()
                .map(ChunkSimilarityView::getDocumentSectionId)
                .collect(Collectors.toSet());

        Map<UUID, DocumentSection> sectionsById;
        try {
            sectionsById = documentSectionRepository.findAllById(sectionIds).stream()
                    .collect(Collectors.toMap(DocumentSection::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Ошибка загрузки секций {} для fileAssetId={}: {}", sectionIds, fileAssetId, e.getMessage(), e);
            throw new RuntimeException("Ошибка загрузки родительских секций", e);
        }

        // Сборка результатов с Parent Document Retrieval
        List<RetrievedChunk> result = filtered.stream()
                .map(chunkView -> {
                    DocumentSection section = sectionsById.get(chunkView.getDocumentSectionId());
                    if (section == null) {
                        log.warn("Секция {} не найдена для чанка {} — пропускаем",
                                chunkView.getDocumentSectionId(), chunkView.getId());
                        return null;
                    }
                    String contextText = resolveContextText(chunkView, section); // режем лишние, если надо
                    return RetrievedChunk.builder()
                            .chunkId(chunkView.getId())
                            .sectionId(section.getId())
                            .headingTitle(section.getHeadingTitle())
                            .contextText(contextText)
                            .similarityScore(chunkView.getSimilarityScore())
                            .pageNumber(section.getSequenceNumber())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        log.info("Retrieval завершён: найдено {} релевантных фрагментов для fileAssetId={} (topK={}, threshold={})",
                result.size(), fileAssetId, topK, threshold);

        return result;
    }

    /**
     * Паттерн Parent Document Retrieval:
     * секция небольшая → берём весь контекст секции (лучше для LLM),
     * секция огромная → берём только чанк с заголовком (не перегружаем промпт).
     */
    private String resolveContextText(ChunkSimilarityView chunk, DocumentSection section) {
        int maxSectionLen = ragProperties.getRetrieval().getMaxSectionLengthForFullContext();
        if (section.getContent() != null && section.getContent().length() < maxSectionLen) {
            String heading = section.getHeadingTitle() != null ? section.getHeadingTitle() : "";
            return heading.isBlank()
                    ? section.getContent()
                    : heading + "\n\n" + section.getContent();
        }
        // Секция слишком большая — берём чанк, добавляем заголовок для контекста
        String heading = section.getHeadingTitle() != null ? section.getHeadingTitle() : "";
        return heading.isBlank()
                ? chunk.getContent()
                : heading + "\n\n" + chunk.getContent();
    }

    /**
     * Конвертирует List<Double> в строку формата "[0.1,0.2,...,0.9]"
     * для передачи в pgvector оператор CAST(:embedding AS vector).
     */
    private String toVectorString(List<Double> embedding) {
        return embedding.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
