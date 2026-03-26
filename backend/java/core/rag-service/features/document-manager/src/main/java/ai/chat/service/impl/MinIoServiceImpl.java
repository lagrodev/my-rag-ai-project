package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.rest.dto.PresignedUploadDto;
import ai.chat.rest.dto.events.DeleteDocumentEvent;
import ai.chat.service.FileStoragePort;
import ai.chat.utils.UtilsGenerator;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinIoServiceImpl implements FileStoragePort {

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    @SneakyThrows
    @Override
    public String uploadFile(String objectName, InputStream inputStream, String contentType, long size) {
        String name = UtilsGenerator.generateUniqueObjectName(objectName);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(name) // Генерируем уникальное имя для объекта
                        .contentType(contentType)
                        .stream(inputStream, size, -1)
                        .build()
        );

        return name;
    }

    @SneakyThrows
    @Override
    public PresignedUploadDto uploadFileForPresign(String originalFilename, String base64Md5Hash)
    {
        String uniqueObjectName = UtilsGenerator.generateUniqueObjectName(originalFilename);

        // Жестко привязываем MD5 к подписи ссылки
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-MD5", base64Md5Hash);

        String uploadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(minioProperties.getBucketName())
                        .object(uniqueObjectName)
                        .expiry(15, TimeUnit.MINUTES)
                        .extraHeaders(headers) // Вшиваем хеш!
                        .build()
        );

        return new PresignedUploadDto(uploadUrl, uniqueObjectName);
    }


    @SneakyThrows
    @Override
    public String getPresignedDownloadUrl(String objectName) {
        // Генерируем ссылку, которая будет жить 1 час (3600 секунд)
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .expiry(1, TimeUnit.HOURS)
                        .build()
        );
    }

    @Override
    @SneakyThrows
    public void removeFile(String minioPath) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(minioPath)
                        .build()
        );
    }

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Гарантирует, что удаление файла произойдет только после успешного коммита транзакции, в которой был удален документ из БД
    public void handleDocumentDeleted(DeleteDocumentEvent event) {
        removeFile(event.minIoPath());
    }


    @SneakyThrows
    @Override
    public InputStream getFile(String bucketName, String objectName) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }


}
