package ai.chat.kafka;

import ai.chat.entity.DocumentStatus;
import ai.chat.entity.OutboxEvent;
import ai.chat.events.DocumentStatusEvent;
import ai.chat.rest.dto.UploadFileEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher
{

    private final KafkaSender kafkaSender;
    private final ApplicationEventPublisher eventPublisher;

    public void handleUploadFileEvent(OutboxEvent event)
    {
        try
        {
            kafkaSender.send("document-parse-tasks", event.getAggregateID().toString(), event).thenAccept(result -> {
                // Выполнится, если Кафка успешно приняла сообщение
                eventPublisher.publishEvent(new DocumentStatusEvent(event.getAggregateID(), DocumentStatus.SENT_TO_PARSER, "File sent to parsing queue", Instant.now()));
            }).exceptionally(ex -> {
                // Выполнится, если Кафка недоступна или отбила сообщение
                log.error("Ошибка асинхронной отправки в Кафку для asset: {}", event.getAggregateID(), ex);

                eventPublisher.publishEvent(new DocumentStatusEvent(event.getAggregateID(), DocumentStatus.FAILED, "Failed to send to parsing queue", Instant.now()));
                return null;
            });

        } catch (Exception e)
        {
            // Сюда мы попадем только при СИНХРОННЫХ ошибках
            // (например, если сериализатор упал ещё до того, как начал отправку)
            // todo: Транзакционный Outbox
            log.error("Критическая синхронная ошибка перед отправкой в Кафку: {}", event.getAggregateID(), e);

            eventPublisher.publishEvent(new DocumentStatusEvent(event.getAggregateID(), DocumentStatus.FAILED, "Internal error before sending to queue", Instant.now()));
        }
    }
}

