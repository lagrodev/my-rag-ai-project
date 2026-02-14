package ai.chat.service;

import java.util.List;
import java.util.UUID;

public interface DocumentIndexingService {
    void indexDocument(UUID documentId, List<String> document);
}
