package ai.chat.service;

import ai.chat.entity.Chunk;
import ai.chat.entity.DocumentSection;

import java.util.List;
import java.util.UUID;

public interface DocumentIndexingService {
    void indexDocument(List<String> document);

    List<Chunk> indexDocumentForSections(List<DocumentSection> sections);
}
