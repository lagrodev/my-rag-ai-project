package ai.chat.service;

import ai.chat.entity.Chunk;
import ai.chat.entity.DocumentSection;
import ai.chat.service.parser.FileParser;
import ai.chat.utils.Tree;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingListener {

    private final FileStoragePort minIoService;
    private final DocumentIndexingService documentIndexingService;

    private final ObjectMapper objectMapper;
    private final DocumentStatusPort port;


    private final DocumentSectionParserService documentSectionParserService;

    @Transactional
    public void handleSuccessfulProcessing(UUID documentId, String resultBucket, String resultFile) {
        port.updateStatus(documentId, "INDEXING", "Indexing...");
        try (InputStream fileStream = minIoService.getFile(resultBucket, resultFile)) {
            Tree parsedDocument = objectMapper.readValue(fileStream, Tree.class);
            port.updateStatus(documentId, "PARSING", "PARSING...");
            log.info("Успешно распарсили документ ID: {}", parsedDocument.getDocumentId());

            log.info("Корневой узел содержит {} потомков", parsedDocument.getRoot().getChildren().size());
            log.info("Распаршенный документ: {}", parsedDocument);

            List<DocumentSection> sections = documentSectionParserService.parseTreeToSections(parsedDocument, documentId);
            log.info("Распарсили на секции: {}", sections);
            port.updateStatus(documentId, "PARSED", "parsed success...");

            List<Chunk> chunks = documentIndexingService.indexDocumentForSections(sections);
            port.updateStatus(documentId, "READY", "doc ready to work...");

            log.info("Распарсили на остатки на чанки: {}", chunks);

        } catch (Exception e) { // ловим все ошибки (чтобы м внутри которые вызовутся рантаймом привели к изменению статуса)
            // catch (Exception e) {
            //    documentService.updateStatus(documentId, DocumentStatus.FAILED, e.getMessage());
            //    log.error("Ошибка обработки документа {}: {}", documentId, e.getMessage(), e);
            //    throw new DocumentProcessingException(documentId, e);
            //}
            log.error(e.getMessage(), e);
            port.updateStatus(documentId, "FAILED", e.getMessage());
            throw new RuntimeException(e); // todo кастомную ошибку


        }


        log.info("successfully processing file {}", resultFile);


    }
}
