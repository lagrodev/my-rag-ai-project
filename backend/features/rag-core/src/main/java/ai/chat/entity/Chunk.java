package ai.chat.entity;


import ai.chat.config.VectorConverter;
import jakarta.persistence.*;
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

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;


    // todo - ссылка на parent_section_id, чтобы в llm улетел весь раздел целиком
    @Convert(converter = VectorConverter.class) // Подключаем конвертер
    @Column(columnDefinition = "vector(1536)")
    private List<Double> embedding;


    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "document_section_id")
    private DocumentSection documentSection;




}
