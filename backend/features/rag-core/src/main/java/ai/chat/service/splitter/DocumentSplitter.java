package ai.chat.service.splitter;

import java.util.List;

public interface DocumentSplitter{
    List<String> split(String text);
}
