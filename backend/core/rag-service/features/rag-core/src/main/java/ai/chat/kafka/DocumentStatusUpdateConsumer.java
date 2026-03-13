package ai.chat.kafka;

import ai.chat.kafka.events.DocumentStatusUpdateEvent;
import ai.chat.service.DocumentStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
// todo ...
public class DocumentStatusUpdateConsumer implements DocumentStatusPort
{
    private final KafkaSender kafkaSender;

    @Override
    public void updateStatus(UUID fileAssetId, String status, String message)
    {
        kafkaSender.send(
                "document-status-updates",
                fileAssetId.toString(),
                new DocumentStatusUpdateEvent(fileAssetId, status, message, Instant.now())
        );
    }
}
