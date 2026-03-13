package ai.chat.service;

import ai.chat.entity.FileAsset;
import ai.chat.rest.dto.ConfirmUploadRequest;
import ai.chat.rest.dto.DocumentDto;
import ai.chat.rest.dto.InitUploadRequest;
import ai.chat.rest.dto.InitUploadResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {

    @Transactional
    InitUploadResponse initDocumentUpload(InitUploadRequest request, UUID userId);

    @Transactional
    DocumentDto confirmDocumentUpload(ConfirmUploadRequest request, UUID userId);


    DocumentDto getDocumentMetadata(UUID id, UUID userId);

    FileAsset getAssetIdOrThrow(UUID documentId, UUID userId);

    String getDocumentDownloadUrl(UUID id, UUID userId);


    Page<@NonNull DocumentDto> findAll(UUID userId,Pageable pageable); // Security фильтр будет на уровне контроллера



    void deleteDocument(UUID id, UUID userId);


    Page<@NonNull DocumentDto> searchByFilename(String filename, UUID userId, Pageable pageable);
}
