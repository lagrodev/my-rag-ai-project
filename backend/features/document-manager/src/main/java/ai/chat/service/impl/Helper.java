package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.entity.Document;
import ai.chat.entity.FileAsset;
import ai.chat.repository.DocumentRepository;
import ai.chat.repository.FileAssetRepository;
import ai.chat.rest.dto.UploadFileEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class Helper
{
    private final DocumentRepository documentRepository;
    private final MinioProperties minioProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final FileAssetRepository fileAssetRepository;

    @Transactional
    public Document saveToDatabaseAndPublishEvent(MultipartFile file, UUID userId,
                                                  String hash, String minIoPath, boolean isNewAsset)
    {
        FileAsset asset = fileAssetRepository.findByHash(hash).orElseGet(() -> {
            FileAsset newAsset = FileAsset.builder()
                    .hash(hash)
                    .minioBucket(minioProperties.getBucketName())
                    .minioPath(minIoPath)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
            return fileAssetRepository.save(newAsset);
        });

        Document userDocument = Document.builder()
                .filename(file.getOriginalFilename())
                .uploadedBy(userId)
                .fileAsset(asset)
                .build();
        userDocument = documentRepository.save(userDocument);

        if (isNewAsset)
        {
            UploadFileEvent event = new UploadFileEvent(
                    asset.getId(),
                    minioProperties.getBucketName(),
                    minIoPath
            );
            eventPublisher.publishEvent(event);
        }

        return userDocument;
    }
}
