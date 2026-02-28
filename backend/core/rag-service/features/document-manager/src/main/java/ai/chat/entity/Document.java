package ai.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "documents", schema = "content") // Важно: schema = "content", мы её создали в init.sql
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// док отдельного пользователя
public class Document extends AbstractEntity {
    @Column(nullable = false)
    private String filename; // как пользователь назвал файл

    // Кто загрузил (пока просто ID, связей @ManyToOne нет, т.к. юзеры в Keycloak)
    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_asset_id", nullable = false)
    private FileAsset fileAsset; // Ссылка на уникальный кешированный файл
}
