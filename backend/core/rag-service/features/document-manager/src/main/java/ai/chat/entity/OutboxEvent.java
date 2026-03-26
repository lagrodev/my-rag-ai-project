package ai.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    private UUID aggregateID; // мб, тут сделать чет по типу - id на сущность, тогда еще сделать какой-то тип, пока просто - file_assets, chat_session и т.п.
    private String aggregateType; // "DOCUMENT_UPLOADED"
    @Column(columnDefinition = "jsonb") // json строка события
    private String payload;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    private OutboxEventState state = OutboxEventState.PENDING;
    // Статус: PENDING, PROCESSED, FAILED


    private String topic;

    public enum OutboxEventState
    {
        PENDING, PROCESSED, FAILED
    }


    private int retryCount = 0;
    private LocalDateTime nextAttemptAt;
    private String errorMessage;
}
