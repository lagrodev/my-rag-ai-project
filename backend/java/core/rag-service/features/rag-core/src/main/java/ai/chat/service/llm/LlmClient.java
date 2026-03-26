package ai.chat.service.llm;

/**
 * Клиент для взаимодействия с языковой моделью (LLM).
 */
public interface LlmClient {

    /**
     * Отправить промпт в LLM и получить текстовый ответ.
     *
     * @param prompt полный промпт с контекстом и вопросом
     * @return текстовый ответ модели
     */
    String complete(String prompt);
}
