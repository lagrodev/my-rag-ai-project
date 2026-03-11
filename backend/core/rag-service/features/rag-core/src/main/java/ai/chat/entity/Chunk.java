package ai.chat.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column
    private float[] embedding;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_section_id")
    private DocumentSection documentSection;


}
