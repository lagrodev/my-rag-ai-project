package ai.chat.rest.dto;

import java.util.UUID;

public record UploadFileEvent(
        UUID id,
        String minIoBucket,
        String filePath
) {
}
