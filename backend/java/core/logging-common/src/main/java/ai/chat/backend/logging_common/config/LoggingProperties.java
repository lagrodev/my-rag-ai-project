package ai.chat.backend.logging_common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.logging")
public class LoggingProperties {

    private boolean requestLogging = true;

    private boolean mdcContext = true;

    private long slowRequestThresholdMs = 1000;

    private int maxBodyLogSize = 1024;
}
