package ai.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

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
    private String minioPath; // Это имя файла в MinIO (мб, тут просто будет сам хеш и тогда вообще нет траблов)

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    // Флаг: распарсился этот файл воркерами или еще в процессе?
    @Column(name = "is_parsed", nullable = false)
    @Builder.Default
    private boolean isParsed = false;
}
