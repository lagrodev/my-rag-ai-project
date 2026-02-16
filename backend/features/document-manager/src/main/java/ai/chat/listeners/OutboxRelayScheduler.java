package ai.chat.listeners;

import ai.chat.entity.OutboxEvent;
import ai.chat.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    // Срабатывает МГНОВЕННО после успешного коммита транзакции
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async // Асинхронно, чтобы не задерживать HTTP-ответ юзеру
    public void publishImmediately(OutboxEvent event) {
        try {
            // Отправляем в Кафку синхронно (внутри асинхронного потока)
            kafkaTemplate.send("document-parse-tasks", event.getAggregateId().toString(), event.getPayload()).get();

            // Если Kafka ответила ACK, обновляем статус в БД
            // Тут нужна отдельная мини-транзакция (RequiresNew или нативный апдейт)
            outboxRepository.updateState(event.getId(), OutboxEvent.State.PROCESSED);
        } catch (Exception e) {
            log.error("Failed to instantly push event to Kafka. Outbox ID: {}", event.getId());
            // Мы НЕ меняем статус на FAILED. Он остается PENDING.
            // Его подберет шедулер-уборщик.
        }
    }

    // Уборщик: работает редко, не насилует БД
    @Scheduled(fixedDelay = 300000) // Раз в 5 минут
    public void processStuckEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        List<OutboxEvent> stuckEvents = outboxRepository.findStuckPendingEvents(threshold);

        for (OutboxEvent event : stuckEvents) {
            try {
                kafkaTemplate.send("document-parse-tasks", event.getAggregateId().toString(), event.getPayload()).get();
                outboxRepository.updateState(event.getId(), OutboxEvent.State.PROCESSED);
            } catch (Exception e) {
                log.error("Sweeper failed to push event {}", event.getId());
                // Тут можно добавить счетчик попыток и переводить в FAILED
            }
        }

    }
}
