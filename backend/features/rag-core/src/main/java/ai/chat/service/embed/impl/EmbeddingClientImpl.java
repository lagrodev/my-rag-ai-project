package ai.chat.service.embed.impl;

import ai.chat.service.embed.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingClientImpl implements EmbeddingClient {
    @Override
    public List<Double> generateEmbedding(String text) {
        return java.util.Collections.nCopies(1536, 0.0);
    }
}
