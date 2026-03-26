package ai.chat.service.llm.impl;

import ai.chat.service.llm.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Primary
public class LlmClientImpl implements LlmClient {

    @Override
    public String complete(String prompt) {
        log.debug("Промпт (первые 200 символов): {}", prompt.substring(0, Math.min(prompt.length(), 200)));
        return "";
    }
}
