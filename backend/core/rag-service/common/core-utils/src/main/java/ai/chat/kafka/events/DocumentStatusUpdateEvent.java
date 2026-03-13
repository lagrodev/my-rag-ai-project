package ai.chat.kafka.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DocumentStatusUpdateEvent(
        @JsonProperty("document_id") UUID fileAssetId,
        String status,
        String message,
        Instant timestamp
)
{
}
