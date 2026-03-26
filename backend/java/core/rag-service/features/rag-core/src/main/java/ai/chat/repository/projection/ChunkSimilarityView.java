package ai.chat.repository.projection;

import java.util.UUID;

/**
 * для спринг нативного запроса
 * маппит строку результата pgvector-запроса с computed similarity_score.
 */
public interface ChunkSimilarityView {
    UUID getId();
    String getContent();
    Integer getChunkIndex();
    UUID getDocumentSectionId();
    Double getSimilarityScore();
}
