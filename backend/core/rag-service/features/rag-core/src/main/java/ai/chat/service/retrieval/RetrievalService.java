package ai.chat.service.retrieval;

import ai.chat.service.retrieval.dto.RetrievedChunk;

import java.util.List;
import java.util.UUID;

/**
 * Retrieval-фаза RAG пайплайна.
 * Паттерн: Parent Document Retrieval —
 *   ищем по маленькому чанку, возвращаем контекст родительской секции.
 */
public interface RetrievalService {

    /**
     * Найти top-K релевантных фрагментов к запросу из документа.
     *
     * @param query       текст вопроса пользователя
     * @param fileAssetId ID файла, по которому ищем
     * @param topK        количество результатов (если не задан — берётся из конфига)
     * @return список найденных фрагментов с контекстом и метаданными
     */
    List<RetrievedChunk> retrieve(String query, UUID fileAssetId, int topK);
}
