package ai.chat.listeners;

import ai.chat.rest.dto.UploadFileEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaSender kafkaSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUploadFileEvent(UploadFileEvent event) {
        try {

            kafkaSender.send("document-parse-tasks", event.id().toString(), event);
        } catch (Exception e) {
            log.error("Критическая ошибка перед отправкой в Кафку для asset: {}", event.id(), e);
            // todo: Транзакционный Outbox
        }
    }
}