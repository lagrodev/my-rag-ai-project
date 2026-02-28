package ai.chat.rest.dto;

import java.util.UUID;

public record ChatRequest(
        UUID userId,
        UUID fileAssetId,
        String query
) {
}
