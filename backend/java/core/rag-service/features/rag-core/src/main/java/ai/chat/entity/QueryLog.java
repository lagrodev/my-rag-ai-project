package ai.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Лог запросов к RAG пайплайну для оценки качества (RAG evaluation).
 * Позволяет анализировать: какие запросы приходят, что возвращается,
 * насколько удовлетворён пользователь, где падает качество поиска.
 */
@Entity
@Table(name = "query_logs", schema = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryLog extends AbstractEntity {

    /** ID файла, по которому был задан вопрос */
    @Column(name = "file_asset_id", nullable = false)
    private UUID fileAssetId;

    /** Пользователь, задавший вопрос */
    @Column(name = "user_id")
    private UUID userId;

    /** Оригинальный вопрос пользователя */
    @Column(name = "query", columnDefinition = "text", nullable = false)
    private String query;

    /** Ответ, сгенерированный LLM */
    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    /** Количество найденных релевантных фрагментов */
    @Column(name = "retrieved_chunks_count")
    private Integer retrievedChunksCount;

    /** Максимальный similarity score среди найденных чанков */
    @Column(name = "max_similarity_score")
    private Double maxSimilarityScore;

    /** Минимальный similarity score среди найденных чанков */
    @Column(name = "min_similarity_score")
    private Double minSimilarityScore;

    /** Источники, использованные при ответе (JSON: [{sectionId, headingTitle, pageNumber, score}]) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources", columnDefinition = "jsonb")
    private String sourcesJson;

    /** Время обработки запроса (мс) */
    @Column(name = "latency_ms")
    private Long latencyMs;

    /** Статус выполнения: SUCCESS / ERROR */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QueryLogStatus status = QueryLogStatus.SUCCESS;

    /** Сообщение об ошибке, если status = ERROR */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** Временная метка запроса */
    @Column(name = "queried_at", nullable = false)
    @Builder.Default
    private LocalDateTime queriedAt = LocalDateTime.now();

    public enum QueryLogStatus {
        SUCCESS, ERROR
    }
}
