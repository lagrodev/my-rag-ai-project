package ai.chat.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record InitUploadRequest(
        @NotBlank String fileName, @NotBlank String md5Base64Hash
)
{
}
