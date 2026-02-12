package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.rest.dto.events.DeleteDocumentEvent;
import ai.chat.rest.dto.events.UploadFileEvent;
import ai.chat.service.MinIoService;
import ai.chat.utils.UtilsGenerator;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinIoServiceImpl implements MinIoService {

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
        // todo слушатель
        UploadFileEvent event = new UploadFileEvent(
                name, contentType, size
        );

        eventPublisher.publishEvent(event);
        return name;
    }

    private final ApplicationEventPublisher eventPublisher;


    @Override
    @SneakyThrows
    public InputStream getFile(String objectName) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
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
}
