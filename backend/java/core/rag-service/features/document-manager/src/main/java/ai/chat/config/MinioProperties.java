package ai.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio") // Читает всё, что начинается с minio.*
@Getter
@Setter
public class MinioProperties {
    private String bucketName; // Сами подтянутся minio.bucket-name
    private String url;
    private String accessKey;
    private String secretKey;
}
