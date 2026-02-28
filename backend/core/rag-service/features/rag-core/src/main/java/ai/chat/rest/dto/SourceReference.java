package ai.chat.rest.dto;

import ai.chat.service.retrieval.dto.RetrievedChunk;
import lombok.Builder;

import java.util.UUID;

/**
 * Ссылка на источник, использованный при генерации ответа.
 * Отображается пользователю для верификации и навигации.
 */
@Builder
public record SourceReference(
        /** ID секции документа */
        UUID sectionId,
        String headingTitle,
        Integer pageNumber,
        Double similarityScore
) {
    /**
     * Создать SourceReference из найденного чанка.
     */
    public static SourceReference from(RetrievedChunk chunk) {
        return SourceReference.builder()
                .sectionId(chunk.sectionId())
                .headingTitle(chunk.headingTitle())
                .pageNumber(chunk.pageNumber())
                .similarityScore(chunk.similarityScore())
                .build();
    }
}
