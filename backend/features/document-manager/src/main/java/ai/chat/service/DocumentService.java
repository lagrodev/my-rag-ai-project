package ai.chat.service;

import ai.chat.rest.dto.DocumentDto;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {
    /**
     * Загрузка файла.
     * 1. Кладет в MinIO
     * 2. Сохраняет запись в Postgres
     * 3. Кидает ивент "Файл готов к индексации"
     */
    DocumentDto uploadDocument(MultipartFile file, UUID userId);

    /**
     * Получение информации о документе (без скачивания байтов).
     */
    DocumentDto getDocumentMetadata(UUID id, UUID userId);

    /**
     * Скачивание самого файла (поток байтов из MinIO).
     */
    Resource downloadDocument(UUID id, UUID userId);

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
