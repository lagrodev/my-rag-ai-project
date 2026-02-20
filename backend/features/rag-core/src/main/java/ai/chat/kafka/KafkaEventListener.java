package ai.chat.kafka;

import ai.chat.kafka.dto.DocumentProcessedEvent;
import ai.chat.service.DocumentIndexingListener;
import ai.chat.service.DocumentIndexingService;
import ai.chat.service.parser.FileParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.swing.text.html.parser.DocumentParser;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final ObjectMapper objectMapper; // Spring сам внедрит Jackson

    private  final DocumentIndexingListener listener;

    @KafkaListener(
            topics = "json-outgoing", // todo вынести название топика в application.yml
            groupId = "chat-ai-group",
            properties = {
                    "spring.json.value.default.type=ai.chat.kafka.dto.DocumentProcessedEvent"
            }
    )
    public void handleDocumentProcessedEvent(String rawJson)
    {


        log.info("Получено событие от Питона для документа: {}", rawJson);
        try
        {
            DocumentProcessedEvent event = objectMapper.readValue(rawJson, DocumentProcessedEvent.class);
            if ("processed".equalsIgnoreCase(event.status()))
            {
                // Питон отработал успешно! Передаем данные в сервис
                listener.handleSuccessfulProcessing(
                        event.documentId(),
                        event.resultBucket(),
                        event.resultFile()
                );
            }
            else
            {
                log.warn("Документ {} обработан со статусом: {}", event.documentId(), event.status());
            }
        } catch (Exception e)
        {
            log.error("Ошибка при обработке сообщения из Кафки для документа, " , e);
            // Тут сообщение может уйти в Dead Letter Queue (DLQ) при должной настройке
        }
    }
}

