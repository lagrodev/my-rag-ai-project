package ai.chat.rest.dto.events;

public record UploadFileEvent(

        String name, String contentType, long size) {
}
