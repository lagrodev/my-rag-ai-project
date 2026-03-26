package ai.chat.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;


public record DocumentDto(
        UUID id,
        String filename,
        LocalDateTime createdAt
)
{
}
