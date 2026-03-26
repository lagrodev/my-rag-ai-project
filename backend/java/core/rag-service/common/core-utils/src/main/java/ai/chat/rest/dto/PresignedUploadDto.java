package ai.chat.rest.dto;

public record PresignedUploadDto (String uploadUrl, String uniqueObjectName)
{
}
