package ai.chat.service.embed.impl;

import ai.chat.service.embed.EmbeddingClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class EmbeddingClientImpl implements EmbeddingClient
{
    @Override
    public List<Double> generateEmbedding(String text){
        return java.util.Collections.nCopies(1536, 0.0);
    }

    @Override
    public List<List<Double>> generateEmbeddingsBatch(List<String> allTexts)
    {
        return allTexts.stream().map(
                this::generateEmbedding
        ).toList();
    }
}
