package ai.chat.service.embed;

import java.util.List;

public interface EmbeddingClient {
    List<Double> generateEmbedding(String text);

    List<List<Double>> generateEmbeddingsBatch(List<String> allTexts);
}
