package ai.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_assets", schema = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// реальный документ
public class FileAsset extends AbstractEntity {
    @Column(name = "hash", unique = true, nullable = false, updatable = false)
    private String hash; // Уникальный SHA-256

    @Column(name = "minio_bucket", nullable = false)
    private String minioBucket;

    @Column(name = "minio_path", nullable = false)
    private String minioPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    // Флаг: распарсился этот файл воркерами или еще в процессе?
    @Deprecated
    @Column(name = "is_parsed", nullable = false)
    @Builder.Default
    private boolean isParsed = false;

    /** Текущий статус обработки документа в RAG пайплайне */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    /** Причина ошибки, если status = FAILED */
    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    /** Время завершения обработки (READY или FAILED) */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
