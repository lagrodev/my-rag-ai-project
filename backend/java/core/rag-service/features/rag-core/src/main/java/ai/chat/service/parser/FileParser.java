package ai.chat.service.parser;


import java.io.InputStream;
import java.util.UUID;

public interface FileParser {
    String extractText(InputStream inputStream, String filename);
}
