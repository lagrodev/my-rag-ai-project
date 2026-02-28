package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.entity.Document;
import ai.chat.entity.FileAsset;
import ai.chat.exceptions.custom.DocumentNotFoundException;
import ai.chat.mapper.DocumentMapper;
import ai.chat.repository.DocumentRepository;
import ai.chat.repository.FileAssetRepository;
import ai.chat.rest.dto.*;
import ai.chat.rest.dto.events.DeleteDocumentEvent;
import ai.chat.service.DocumentService;
import ai.chat.service.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


// 2 варика: есть файл, нет файла. Загрузка через клиент и получение хеша и некст проверка и т.п.
    @Transactional
    @Override
    public InitUploadResponse initDocumentUpload(InitUploadRequest request, UUID userId) {

        FileAsset asset = fileAssetRepository.findByHash(request.md5Base64Hash()).orElse(null);

        if (asset != null) {
            Document userDocument = saveToDatabaseAndPublishEvent(
                    request.fileName(),
                    asset,
                    userId, asset.getMinioPath(), false
            );

            return new InitUploadResponse(null, null, true, documentMapper.toDto(userDocument));
        }

        // 2. Файла нет.
        PresignedUploadDto presigned = minIoService.uploadFileForPresign(request.fileName(), request.md5Base64Hash());

        return new InitUploadResponse(presigned.uploadUrl(), presigned.uniqueObjectName(), false, null);
    }


    @Transactional
    protected Document saveToDatabaseAndPublishEvent(
            String originalName,
            FileAsset asset,
            UUID userId,
            String minIoPath, boolean isNewAsset
    )
    {
        Document userDocument = Document.builder()
                .filename(originalName)
                .uploadedBy(userId)
                .fileAsset(asset)
                .build();

        userDocument = documentRepository.save(userDocument);

        if (isNewAsset){
            UploadFileEvent event = new UploadFileEvent(
                    asset.getId(),
                    minioProperties.getBucketName(),
                    minIoPath
            );
            eventPublisher.publishEvent(event);
        }

        return userDocument;
    }


    @Transactional
    @Override
    public DocumentDto confirmDocumentUpload(ConfirmUploadRequest request, UUID userId) {

        FileAsset asset = fileAssetRepository.findByHash(request.md5Base64Hash()).orElse(null);

        boolean isNewAsset = false;
        if (asset == null) {
            asset = FileAsset.builder()
                    .minioBucket(minioProperties.getBucketName())
                    .minioPath(request.minioPath())
                    .hash(request.md5Base64Hash())
                    .fileSize(request.fileSize())
                    .contentType(request.contentType())
                    .build();
            asset = fileAssetRepository.save(asset);
            isNewAsset = true;
        }

        Document userDocument = saveToDatabaseAndPublishEvent(
                request.filename(), asset, userId, request.minioPath(), isNewAsset
        );

        return documentMapper.toDto(userDocument);
    }




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
