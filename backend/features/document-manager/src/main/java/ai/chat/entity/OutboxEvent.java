package ai.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(schema = "content", name = "outbox_events")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends AbstractEntity
{
    private UUID aggregateId;
    private String type; // "DOCUMENT_UPLOADED"
    @Column(columnDefinition = "jsonb") // Если PostgreSQL
    private String payload;

    @Enumerated(EnumType.STRING)
    private State state;
    // Статус: PENDING, PROCESSED, FAILED

    public enum State
    {
        PENDING, PROCESSED, FAILED
    }
}
