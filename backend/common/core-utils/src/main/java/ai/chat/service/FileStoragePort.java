package ai.chat.service;

import java.io.InputStream;

public interface FileStoragePort {
    String uploadFile(
            String objectName, InputStream inputStream, String contentType, long size
    );
    String getPresignedDownloadUrl(String objectName);

    void removeFile(String minioPath);
}
