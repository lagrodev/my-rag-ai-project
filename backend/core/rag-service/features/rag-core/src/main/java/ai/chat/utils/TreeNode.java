package ai.chat.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class TreeNode {
    private String type;

    private String content;

    private Heading heading;

    private List<TreeNode> children;

    @Data
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Heading
    {
        private Long depth;
        private String title;

        @JsonProperty("page_starts_at")
        private Long pageStartsAt;


    }

}
