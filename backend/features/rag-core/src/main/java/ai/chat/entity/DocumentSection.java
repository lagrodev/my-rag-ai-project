package ai.chat.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLLTreeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Entity
@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "content")
public class DocumentSection extends AbstractEntity
{
    @Column(name = "file_asset_id", nullable = false)
    private UUID fileAssetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_section_id")
    private DocumentSection parentSection;

    @Column(name = "heading_title")
    private String headingTitle;

    @Column(name = "heading_depth")
    private Integer headingDepth;

    @Column(columnDefinition = "text")
    private String content;


    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Type(PostgreSQLLTreeType.class)
    @Column(name = "path", columnDefinition = "ltree")
    private String path;

}
