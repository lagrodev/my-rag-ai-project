package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.entity.Document;
import ai.chat.entity.FileAsset;
import ai.chat.entity.OutboxEvent;
import ai.chat.exceptions.custom.DocumentNotFoundException;
import ai.chat.mapper.DocumentMapper;
import ai.chat.repository.DocumentRepository;
import ai.chat.repository.FileAssetRepository;
import ai.chat.rest.dto.DocumentDto;
import ai.chat.rest.dto.events.DeleteDocumentEvent;
import ai.chat.rest.dto.UploadFileEvent;
import ai.chat.service.DocumentService;
import ai.chat.service.FileStoragePort;
import ai.chat.utils.UtilsGenerator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl implements DocumentService
{

    private final DocumentRepository documentRepository;
    private final MinioProperties minioProperties;
    private final FileStoragePort minIoService;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final FileAssetRepository fileAssetRepository;


    /**
     * Загрузка файла.
     * 1. Кладет в MinIO
     * 2. Сохраняет запись в Postgres
     * 3. Кидает ивент "Файл готов к индексации"
     */

    @Override
    public DocumentDto uploadDocument(MultipartFile file, UUID userId)
    {

        OutboxEvent event = new OutboxEvent();


        Path tempFile = null;
        try
        {
            tempFile = Files.createTempFile("upload-", ".tmp");

            file.transferTo(tempFile);
            String hash = UtilsGenerator.getHash256FromFile(tempFile.toFile());

            boolean isNewAsset = false;
            String minIoPath;
            FileAsset asset = fileAssetRepository.findByHash(hash).orElse(null);

            if (asset == null)
            {
                try (InputStream is = Files.newInputStream(tempFile))
                {
                    minIoPath = minIoService.uploadFile(hash + ".pdf", is, file.getContentType(), file.getSize());
                    isNewAsset = true;
                }
            }
            else
            {
                minIoPath = asset.getMinioPath();
            }

            Document userDocument = helper.saveToDatabaseAndPublishEvent(
                    file, userId, hash, minIoPath, isNewAsset
            );

            return documentMapper.toDto(userDocument);


        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (tempFile != null)
            {
                try
                {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private final Helper helper;

    @Override
    public DocumentDto getDocumentMetadata(UUID id, UUID userId)
    {
        var doc = getMyDocument(id, userId);
        return documentMapper.toDto(doc);
    }

    @Override
    public String getDocumentDownloadUrl(UUID id, UUID userId)
    {
        var doc = getMyDocument(id, userId);
        return minIoService.getPresignedDownloadUrl(doc.getFileAsset().getMinioPath());
    }

    @Override
    public Page<@NonNull DocumentDto> findAll(UUID userId, Pageable pageable)
    {
        return documentRepository.findAllByUploadedBy(userId, pageable).map(documentMapper::toDto);
    }

    private Document getMyDocument(UUID id, UUID userId)
    {

        return documentRepository.findByIdAndUserIdWithAsset(id, userId).orElseThrow(
                () -> new DocumentNotFoundException("Document not found with id: " + id)
        );
    }

    @Override
    public void deleteDocument(UUID id, UUID userId)
    {

        var doc = getMyDocument(id, userId);
        // Удаляем док
        documentRepository.delete(doc);

        DeleteDocumentEvent documentEvent = new DeleteDocumentEvent(
                doc.getId(),
                userId,
                null
        );

        eventPublisher.publishEvent(documentEvent);

        // TODO: событие - отдельным ивентом
        // тут событие сделать, чтобы потом логер добавить и видеть, кто че удалил, а то сейчас просто удалится и все, а так будет видно, что удалил и кто удалил
    }

    @Override
    public Page<@NonNull DocumentDto> searchByFilename(String filename, UUID userId, Pageable pageable)
    {
        return documentRepository.findByFilenameContainingIgnoreCaseAndUploadedBy(filename, userId, pageable).map(documentMapper::toDto);
    }
}
