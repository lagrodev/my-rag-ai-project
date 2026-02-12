package ai.chat.rag.service.parser.impl;

import ai.chat.rag.service.parser.FileParser;
import lombok.SneakyThrows;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class FileParserImpl implements FileParser {

    @Override
    @SneakyThrows
    public String extractText(InputStream inputStream, String filename) {

        BodyContentHandler handler = new BodyContentHandler(-1); // -1 для неограниченного размера

        Metadata metadata = new Metadata();
        metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        try {
            parser.parse(inputStream, handler, metadata, context);
        } catch (Exception e) {
            // Если файл битый или зашифрован, можно вернуть пустую строку или кинуть ошибку дальше
            throw new RuntimeException("Failed to parse file: " + filename, e);
        }

        return handler.toString();
    }
}
