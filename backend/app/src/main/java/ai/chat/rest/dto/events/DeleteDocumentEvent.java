package ai.chat.rest.dto.events;

import java.util.UUID;

public record DeleteDocumentEvent(
        UUID documentId,
        UUID userId,
        String minIoPath
) {
}
