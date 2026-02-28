package ai.chat.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSender {

    // Используем Object, так как Spring Kafka сама сериализует объекты в JSON
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String key, Object payload) {
        log.trace("Отправка в топик={} key={} payload={}", topic, key, payload.getClass().getSimpleName());

        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Ошибка отправки в Кафку: topic={} key={}: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Успешно отправлено: topic={} partition={} offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}