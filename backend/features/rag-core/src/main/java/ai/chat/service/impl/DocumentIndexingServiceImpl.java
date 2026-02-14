package ai.chat.service.impl;

import ai.chat.entity.Chunk;
import ai.chat.repository.ChunkRepository;
import ai.chat.service.DocumentIndexingService;
import ai.chat.service.embed.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private final ChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    @Override
    public void indexDocument(UUID documentId, List<String> document) {
        List<Chunk> chunks = document.stream()
                .map(textPart -> {
                        var vector = embeddingClient.generateEmbedding(textPart);
                        return Chunk.builder()
                                .documentId(documentId)
                                .content(textPart)
                                .embedding(vector)
                                .build();
                }).toList();
        chunkRepository.saveAll(chunks);

    }
}
