package ai.chat.service.parser;


import java.io.InputStream;

public interface FileParser {
    String extractText(InputStream inputStream, String filename);
}
