package ai.chat.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", schema = "content") // Важно: schema = "content", мы её создали в init.sql
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends AbstractEntity {
    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType; // например "application/pdf"

    // Данные для MinIO
    @Column(name = "minio_bucket", nullable = false)
    private String minioBucket;

    @Column(name = "minio_path", nullable = false) // Путь внутри бакета (чтобы не терять)
    private String minioPath;

    @Column(name = "file_size")
    private Long fileSize;

    // Кто загрузил (пока просто ID, связей @ManyToOne нет, т.к. юзеры в Keycloak)
    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
