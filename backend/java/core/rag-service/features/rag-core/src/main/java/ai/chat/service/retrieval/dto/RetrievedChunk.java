package ai.chat.service.retrieval.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Результат retrieval-фазы RAG пайплайна.
 * Используется паттерн Parent Document Retrieval:
 * - поиск ведётся по маленькому чанку (точнее семантически),
 * - в LLM отправляется текст родительской секции (больше контекста).
 */
@Builder
public record RetrievedChunk(

        UUID chunkId,
        UUID sectionId,
        String headingTitle,
        String contextText,
        Double similarityScore,
        Integer pageNumber

) {


}
