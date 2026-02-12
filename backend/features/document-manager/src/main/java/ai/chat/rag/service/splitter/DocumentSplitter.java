package ai.chat.rag.service.splitter;

import java.util.List;

public interface DocumentSplitter{
    List<String> split(String text);
}
