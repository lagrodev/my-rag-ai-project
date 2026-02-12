package ai.chat.service.impl;

import ai.chat.config.MinioProperties;
import ai.chat.entity.Document;
import ai.chat.mapper.custom.DocumentNotFoundException;
import ai.chat.mapper.custom.FileReadError;
import ai.chat.mapper.DocumentMapper;
import ai.chat.repository.DocumentRepository;
import ai.chat.rest.dto.DocumentDto;
import ai.chat.rest.dto.events.DeleteDocumentEvent;
import ai.chat.service.DocumentService;
import ai.chat.service.MinIoService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final MinIoService minIoService;
    private final MinioProperties minioProperties;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Загрузка файла.
     * 1. Кладет в MinIO
     * 2. Сохраняет запись в Postgres
     * 3. Кидает ивент "Файл готов к индексации"
     */
    @Override
    public DocumentDto uploadDocument(MultipartFile file, UUID userId) {
        try {
            String name = minIoService.uploadFile(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize()
            );
            Document document = Document.builder()
                    .filename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .minioBucket(minioProperties.getBucketName())
                    .minioPath(name)
                    .uploadedBy(userId)
                    .build();
            document = documentRepository.save(document);
            return documentMapper.toDto(document);
        } catch (IOException e) {
            throw new FileReadError("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public DocumentDto getDocumentMetadata(UUID id, UUID userId) {
        var doc = getMyDocument(id, userId);
        return documentMapper.toDto(doc);
    }

    @Override
    public Resource downloadDocument(UUID id, UUID userId) {
        var doc = getMyDocument(id, userId);
        try {
            String file = doc.getMinioPath();
            InputStream inputStream = minIoService.getFile(file);
            return new InputStreamResource(inputStream);
        } catch (Exception e) {
            throw new FileReadError("Failed to download file: " + e.getMessage());
        }
    }

    @Override
    public Page<@NonNull DocumentDto> findAll(UUID userId,Pageable pageable) {
        return documentRepository.findAllByUploadedBy(userId, pageable).map(documentMapper::toDto);
    }

    private Document getMyDocument(UUID id, UUID userId) {
        var doc = documentRepository.findById(id).orElseThrow(
                () -> new DocumentNotFoundException("Document not found with id: " + id)
        );
        if (!doc.getUploadedBy().equals(userId)) {
            throw new DocumentNotFoundException("Document not found with id: " + id);
        }
        return doc;
    }

    @Override
    public void deleteDocument(UUID id, UUID userId) {

        var doc = getMyDocument(id, userId);
        // Удаляем док
        documentRepository.delete(doc);

        DeleteDocumentEvent documentEvent = new DeleteDocumentEvent(
                doc.getId(),
                userId,
                doc.getMinioPath()
        );

        eventPublisher.publishEvent(documentEvent);

        // TODO: событие - отдельным ивентом
        // тут событие сделать, чтобы потом логер добавить и видеть, кто че удалил, а то сейчас просто удалится и все, а так будет видно, что удалил и кто удалил
    }

    @Override
    public Page<@NonNull DocumentDto> searchByFilename(String filename, UUID userId, Pageable pageable) {
        return documentRepository.findByFilenameContainingIgnoreCaseAndUploadedBy(filename, userId, pageable).map(documentMapper::toDto);
    }
}
