package ai.chat.service.logging;

import ai.chat.rest.dto.ChatResponse;
import ai.chat.service.retrieval.dto.RetrievedChunk;

import java.util.List;
import java.util.UUID;

/**
 * Сервис логирования RAG запросов для оценки качества (RAG evaluation / observability).
 */
public interface QueryLogService {

    /**
     * Асинхронно сохранить успешный запрос со всеми метриками.
     *
     * @param fileAssetId ID документа
     * @param userId      ID пользователя (может быть null)
     * @param query       вопрос пользователя
     * @param response    ответ RAG пайплайна
     * @param chunks      найденные чанки (для метрик similarity)
     * @param latencyMs   время обработки (мс)
     */
    void logSuccess(
            UUID fileAssetId,
            UUID userId,
            String query,
            ChatResponse response,
            List<RetrievedChunk> chunks,
            long latencyMs
    );

    /**
     * Асинхронно сохранить запрос, завершившийся ошибкой.
     *
     * @param fileAssetId  ID документа
     * @param userId       ID пользователя (может быть null)
     * @param query        вопрос пользователя
     * @param errorMessage описание ошибки
     * @param latencyMs    время до ошибки (мс)
     */
    void logError(UUID fileAssetId, UUID userId, String query, String errorMessage, long latencyMs);
}
