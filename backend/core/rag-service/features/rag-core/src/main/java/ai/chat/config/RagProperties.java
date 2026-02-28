package ai.chat.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки RAG пайплайна — все пороги и дефолты вынесены в application.yml.
 * Пример конфигурации:
 * <pre>
 * rag:
 *   retrieval:
 *     default-top-k: 5
 *     similarity-threshold: 0.7
 *     max-section-length-for-full-context: 2000
 *   llm:
 *     max-context-length: 8000
 *   conversation:
 *     max-history-messages: 10
 *     max-history-tokens: 3000
 * </pre>
 */
@Getter
@Setter
@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Retrieval retrieval;
    private Llm llm;
    private Conversation conversation;

    @Getter
    @Setter
    public static class Retrieval {
        /** Сколько чанков брать по умолчанию */
        private int defaultTopK = 5;
        /** Минимальный порог сходства — чанки ниже порога игнорируются */
        private double similarityThreshold = 0.7;
        /** Максимальная длина секции, при которой берём её целиком вместо чанка */
        private int maxSectionLengthForFullContext = 2000;
    }

    @Getter
    @Setter
    public static class Llm {
        /** Максимальный размер контекста (символов), отправляемого в LLM */
        private int maxContextLength = 8000;
    }

    @Getter
    @Setter
    public static class Conversation {
        /** Максимальное количество сообщений истории в промпте */
        private int maxHistoryMessages = 10;
        /** Максимальное количество токенов истории (приблизительно: 1 токен ≈ 4 символа) */
        private int maxHistoryTokens = 3000;
    }
}
