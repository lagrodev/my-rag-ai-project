package ai.chat.service.impl;

import ai.chat.entity.Chunk;
import ai.chat.entity.DocumentSection;
import ai.chat.repository.ChunkRepository;
import ai.chat.service.DocumentIndexingService;
import ai.chat.service.embed.EmbeddingClient;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private static final int MAX_SEGMENT_SIZE = 1000;
    private static final int MAX_OVERLAP_SIZE = 200;
    private final ChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    @Override
    public List<Chunk> indexDocumentForSections(List<DocumentSection> documentSections) {

        var splitter = DocumentSplitters.recursive(MAX_SEGMENT_SIZE, MAX_OVERLAP_SIZE);

        List<Chunk> allChunks = documentSections.stream()
                .filter(section -> section.getContent() != null && !section.getContent().isBlank())

                .flatMap(documentSection -> {

                    Document doc = Document.from(documentSection.getContent());

                    List<TextSegment> segments = splitter.split(doc);

                    List<Chunk> sectionChunks = new ArrayList<>();

                    for (int i = 0; i < segments.size(); i++) {
                        String chunkText = segments.get(i).text();

                        sectionChunks.add(Chunk.builder()
                                        .chunkIndex(i + 1)
                                        .content(chunkText)
                                        .documentSection(documentSection)
//                                .embedding(embeddingClient.generateEmbedding(chunkText))
                                        .build()
                        );
                    }

                    return sectionChunks.stream();
                })
                .toList();

        if (allChunks.isEmpty()) {
            return allChunks;
        }

        List<String> allTexts = allChunks.stream()
                .map(Chunk::getContent)
                .toList();

        List<List<Double>> embeddingsBatch = embeddingClient.generateEmbeddingsBatch(allTexts);

        for (int i = 0; i < embeddingsBatch.size(); i++) {
            List<Double> sourceEmbedding = embeddingsBatch.get(i);
            float[] floatEmbedding = new float[sourceEmbedding.size()];
            int j = 0;

            for (Double d : sourceEmbedding) {
                floatEmbedding[j++] = d.floatValue();
            }

            allChunks.get(i).setEmbedding(floatEmbedding);
        }

        return chunkRepository.saveAll(allChunks);
    }

    @Override
    public void indexDocument(List<String> document) {
        List<Chunk> chunks = document.stream()
                .map(textPart -> {
                    var vector = embeddingClient.generateEmbedding(textPart);
                    return Chunk.builder()
                            .content(textPart)
//                            .embedding(vector)
                            .build();
                }).toList();
        chunkRepository.saveAll(chunks);
    }

}
