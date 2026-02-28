package ai.chat.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DocumentProcessedEvent(
        @JsonProperty("document_id") UUID documentId,
        @JsonProperty("result_bucket") String resultBucket,
        @JsonProperty("result_file") String resultFile,
        String status
)
{
}
