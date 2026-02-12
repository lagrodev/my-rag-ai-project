package ai.chat.rest.dto;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        String filename,
        String contentType,
        Long fileSize,
        Boolean isIndexed,
        LocalDateTime createdAt
) {
}
