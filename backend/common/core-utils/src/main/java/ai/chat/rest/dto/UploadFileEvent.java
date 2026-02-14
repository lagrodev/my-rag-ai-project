package ai.chat.rest.dto;

import java.util.UUID;

public record UploadFileEvent(
        String fileName , String filePath, String contentType, long size, UUID documentId
) {
}
