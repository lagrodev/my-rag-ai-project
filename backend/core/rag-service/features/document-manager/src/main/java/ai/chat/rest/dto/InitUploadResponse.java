package ai.chat.rest.dto;

public record InitUploadResponse(
    String uploadUrl,
    String minioPath,
    boolean isAlreadyUploaded,
    DocumentDto document // Будет заполнено, если файл уже есть в базе
) {}
