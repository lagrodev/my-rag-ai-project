package ai.chat.utils;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Tree
{
    @JsonProperty("document_id")
    private String documentId;
    @JsonProperty("source_file")
    private String sourceFile;

    @JsonProperty("tree")
    TreeNode root;

    @JsonProperty("processed_at")
    private Instant processedAt;

}
