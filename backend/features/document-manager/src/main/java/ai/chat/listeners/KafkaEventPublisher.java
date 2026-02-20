package ai.chat.listeners;

import ai.chat.rest.dto.UploadFileEvent;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
// todo улучшить реализацию
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUploadFileEvent(UploadFileEvent event)
    {
        try
        {
            kafkaTemplate.send("document-parse-tasks", event.id().toString(), event);
            log.info("Event sent to Kafka for asset: {}", event.id());
        }
        catch (Exception e){
            log.error("Error while sending event to Kafka for asset: {}", event.id());
            // todo: добавить @Scheduled джобу, которая раз в 2 секунды делает
            //  SELECT * FROM outbox_events WHERE status = 'PENDING' LIMIT 100,
            //  отправляет их в KafkaTemplate.send(), ждет подтверждения от Кафки (ACK) и обновляет статус на PROCESSED (или удаляет запись).
        }
    }
}
