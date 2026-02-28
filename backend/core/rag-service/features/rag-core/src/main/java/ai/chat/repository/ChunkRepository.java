package ai.chat.repository;

import ai.chat.entity.Chunk;
import ai.chat.repository.projection.ChunkSimilarityView;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<@NonNull Chunk, @NonNull UUID> {

    /**
     * Топ-K чанков по косинусному сходству.
     * Возвращает проекцию с similarity_score, чтобы не терять вычисленное значение.
     *
     * embedding передаётся как строка вида "[0.1,0.2,...]" и кастуется к vector.
     */
    @Query(value = """
            SELECT c.id                                                              AS id,
                   c.content                                                         AS content,
                   c.chunk_index                                                     AS chunkIndex,
                   c.document_section_id                                             AS documentSectionId,
                   1 - (c.embedding <=> CAST(:embedding AS vector))                  AS similarityScore
            FROM content.document_chunks c
                     JOIN content.document_section ds ON c.document_section_id = ds.id
            WHERE ds.file_asset_id = :fileAssetId
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarityView> findTopKByCosineSimilarity(
            @Param("embedding") String embedding,
            @Param("fileAssetId") UUID fileAssetId,
            @Param("topK") int topK
    );
}
