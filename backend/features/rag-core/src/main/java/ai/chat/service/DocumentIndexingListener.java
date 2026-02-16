package ai.chat.service;

import ai.chat.rest.dto.UploadFileEvent;
import ai.chat.service.parser.FileParser;
import ai.chat.service.splitter.DocumentSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingListener {

    private final FileParser fileParser;
    private final DocumentSplitter documentSplitter;
    private final FileStoragePort minIoService;
    private final DocumentIndexingService documentIndexingService;




    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // тут просто если что через кафку сделать, чтобы он асинхронно обрабатывал, а не в рамках одного запроса
    // @KafkaListener(topics = "document-uploaded", groupId = "rag-service")
    public void handleDocumentUploaded(UploadFileEvent event) {
//        log.info("Received DocumentUploadedEvent for file: {}", event.filePath());
//        try {
//            // 1. Парсим файл и получаем текст
//            InputStream fileStream = minIoService.getFile(event.filePath());
//
//            String documentText = fileParser.extractText(fileStream, event.fileName());
//            log.info("Parsed document text for file: {}", event.fileName());
//
//            // 2. Разбиваем текст на части
//            var documentParts = documentSplitter.split(documentText);
//            log.info("Split document into {} parts for file: {}", documentParts.size(), event.filePath());
//
//            // 3. Индексируем части (здесь можно вызвать сервис для сохранения в базу или векторной БД)
//            documentIndexingService.indexDocument(event.documentId(),documentParts);
//
//            log.info("Indexed document parts for file: {}", event.filePath());
//        } catch (Exception e) {
//            log.error("Error processing DocumentUploadedEvent for file: {}", event.filePath(), e);
//        }
    }

}
