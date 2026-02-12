package ai.chat.rag.service.splitter.impl;

import ai.chat.rag.service.parser.FileParser;
import ai.chat.rag.service.splitter.DocumentSplitter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DocumentSplitterImpl implements DocumentSplitter{

    // todo: умеый механизм, как рассчитывать длину сегментов
    private static final int MAX_SEGMENT_SIZE = 1000;
    private static final int MAX_OVERLAP_SIZE = 200;

//    private final FileParser documentParser;

    @Override
    public List<String> split(String text){
        Document document = Document.from(text);

        var splitter = DocumentSplitters.recursive(MAX_SEGMENT_SIZE, MAX_OVERLAP_SIZE);

        List<TextSegment> segments = splitter.split(document);

        // 4. Превращаем обратно в строки (пока нам нужен только текст)
        return segments.stream()
                .map(TextSegment::text)
                .toList();
    }
}
