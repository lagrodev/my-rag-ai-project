package ai.chat.service;

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
    /**
     * Загрузка файла.
     * 1. Кладет в MinIO
     * 2. Сохраняет запись в Postgres
     * 3. Кидает ивент "Файл готов к индексации"
     */


    @Transactional
    InitUploadResponse initDocumentUpload(InitUploadRequest request, UUID userId);

    @Transactional
    DocumentDto confirmDocumentUpload(ConfirmUploadRequest request, UUID userId);

    /**
     * Получение информации о документе (без скачивания байтов).
     */
    DocumentDto getDocumentMetadata(UUID id, UUID userId);


    String getDocumentDownloadUrl(UUID id, UUID userId);

    /**
     * Список документов пользователя с пагинацией.
     */
    Page<@NonNull DocumentDto> findAll(UUID userId,Pageable pageable); // Security фильтр будет на уровне контроллера

    /**
     * Удаление (из базы, из MinIO и из векторного индекса).
     */

    void deleteDocument(UUID id, UUID userId);

    /**
     * Поиск по имени файла (простой like).
     */
    Page<@NonNull DocumentDto> searchByFilename(String filename, UUID userId, Pageable pageable);
}
