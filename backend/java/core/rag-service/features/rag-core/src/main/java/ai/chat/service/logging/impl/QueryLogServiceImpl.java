package ai.chat.service.logging.impl;

import ai.chat.entity.QueryLog;
import ai.chat.repository.QueryLogRepository;
import ai.chat.service.logging.QueryLogService;
import ai.chat.rest.dto.ChatResponse;
import ai.chat.rest.dto.SourceReference;
import ai.chat.service.retrieval.dto.RetrievedChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Асинхронное логирование — не должно замедлять ответ пользователю.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryLogServiceImpl implements QueryLogService {

    private final QueryLogRepository queryLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(
            UUID fileAssetId,
            UUID userId,
            String query,
            ChatResponse response,
            List<RetrievedChunk> chunks,
            long latencyMs
    ) {
        try {
            Double maxScore = chunks.stream()
                    .map(RetrievedChunk::similarityScore)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())// jaba будет натурально сравнивать
                    .orElse(null);
            Double minScore = chunks.stream()
                    .map(RetrievedChunk::similarityScore)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            String sourcesJson = serializeSources(response.sources());// Spring поймет это как json

            QueryLog entry = QueryLog.builder()
                    .fileAssetId(fileAssetId)
                    .userId(userId)
                    .query(query)
                    .answer(response.answer())
                    .retrievedChunksCount(chunks.size())
                    .maxSimilarityScore(maxScore)
                    .minSimilarityScore(minScore)
                    .sourcesJson(sourcesJson)
                    .latencyMs(latencyMs)
                    .status(QueryLog.QueryLogStatus.SUCCESS)
                    .build();

            queryLogRepository.save(entry);
            log.debug("QueryLog сохранён: fileAssetId={}, latencyMs={}, chunks={}", fileAssetId, latencyMs, chunks.size());

        } catch (Exception e) {
            // Логируем ошибку логирования — не бросаем наверх (не ломаем основной флоу)
            log.error("Ошибка сохранения QueryLog для fileAssetId={}: {}", fileAssetId, e.getMessage(), e);
        }
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(UUID fileAssetId, UUID userId, String query, String errorMessage, long latencyMs) {
        try {
            QueryLog entry = QueryLog.builder()
                    .fileAssetId(fileAssetId)
                    .userId(userId)
                    .query(query)
                    .retrievedChunksCount(0)
                    .latencyMs(latencyMs)
                    .status(QueryLog.QueryLogStatus.ERROR)
                    .errorMessage(errorMessage)
                    .build();

            queryLogRepository.save(entry);
            log.debug("QueryLog[ERROR] сохранён: fileAssetId={}, error={}", fileAssetId, errorMessage);

        } catch (Exception e) {
            log.error("Ошибка сохранения QueryLog[ERROR] для fileAssetId={}: {}", fileAssetId, e.getMessage(), e);
        }
    }

    private String serializeSources(List<SourceReference> sources) {
        if (sources == null || sources.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось сериализовать sources в JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
