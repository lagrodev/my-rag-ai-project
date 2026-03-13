package ai.chat.kafka;

import ai.chat.entity.DocumentStatus;
import ai.chat.events.DocumentStatusEvent;
import ai.chat.kafka.events.DocumentStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusKafkaAdapter
{

    private final ApplicationEventPublisher publisher;


    @KafkaListener(topics = "document-status-updates", groupId = "chat-ai-group")
    public void handleStatusUpdate(@Payload DocumentStatusUpdateEvent rawEvent)
    {
        try
        {
            DocumentStatus status = DocumentStatus.valueOf(rawEvent.status().toUpperCase());

            DocumentStatusEvent event = new DocumentStatusEvent(
                    rawEvent.fileAssetId(),
                    status,
                    rawEvent.message(),
                    rawEvent.timestamp()
            );

            publisher.publishEvent(event);
            log.debug("Published internal event for document: {}", rawEvent.fileAssetId());
        } catch (IllegalArgumentException e)
        {
            log.error("Unknown status received: {}", rawEvent.status());
            throw new IllegalArgumentException("Unknown status received: " + rawEvent.status());
        }

    }
}

