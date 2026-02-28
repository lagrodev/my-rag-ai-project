package ai.chat.service;

import ai.chat.rest.dto.PresignedUploadDto;
import lombok.SneakyThrows;

import java.io.InputStream;

public interface FileStoragePort
{
    String uploadFile(
            String objectName, InputStream inputStream, String contentType, long size
    );


    PresignedUploadDto uploadFileForPresign(String originalFilename, String base64Md5Hash);

    String getPresignedDownloadUrl(String objectName);

    void removeFile(String minioPath);


    InputStream getFile(String bucketName, String objectName);
}
