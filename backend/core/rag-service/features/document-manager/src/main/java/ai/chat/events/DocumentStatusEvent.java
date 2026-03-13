package ai.chat.events;

import ai.chat.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentStatusEvent(
        UUID fileAssetId,
        DocumentStatus status,
        String message,
        Instant timestamp
)
{
}
