package ai.chat.entity;


import ai.chat.config.VectorConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", schema = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chunk extends AbstractEntity {
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Convert(converter = VectorConverter.class) // Подключаем конвертер
    @Column(columnDefinition = "vector")
    private List<Double> embedding;
}
