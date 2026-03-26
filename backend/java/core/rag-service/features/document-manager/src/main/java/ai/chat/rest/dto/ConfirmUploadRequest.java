package ai.chat.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmUploadRequest(
        @NotBlank String filename,
        @NotBlank String md5Base64Hash,
        @NotBlank String minioPath,
        @NotNull @Positive Long fileSize,
        @NotBlank String contentType
)
{

}