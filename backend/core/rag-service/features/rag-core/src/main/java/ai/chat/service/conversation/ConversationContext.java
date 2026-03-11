package ai.chat.service.conversation;

import ai.chat.config.RagProperties;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Контекст текущего диалога пользователя с документом.
 *
 * Хранит историю последних N сообщений (ограничено по количеству и токенам).
 * Используется для добавления истории в промпт LLM:
 * это позволяет задавать уточняющие вопросы
 *
 * todo Для production использование Redis, для хранения между запросами.
 */
@Getter
public class ConversationContext {

    private final UUID fileAssetId;
    private final UUID userId;
    private final List<Message> history = new ArrayList<>();

    private final int maxMessages;
    private final int maxTokens;

    public ConversationContext(UUID fileAssetId, UUID userId, RagProperties ragProperties) {
        this.fileAssetId = fileAssetId;
        this.userId = userId;
        this.maxMessages = ragProperties.getConversation().getMaxHistoryMessages();
        this.maxTokens = ragProperties.getConversation().getMaxHistoryTokens();
    }

    /**
     * Добавить сообщение в историю.
     * После добавления обрезает историю по лимитам.
     */
    public void addMessage(Message.Role role, String content) {
        history.add(new Message(role, content));
        trim();
    }

    /**
     * Получить историю в ограниченном виде для вставки в промпт.
     * Возвращает последние сообщения, не превышающие оба лимита.
     */
    public List<Message> getHistoryForPrompt() {
        if (history.isEmpty()) {
            return Collections.emptyList();
        }

        List<Message> result = new ArrayList<>();
        int tokenCount = 0;

        // Идём с конца (самые свежие сообщения важнее)
        List<Message> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);

        for (Message msg : reversed) {
            if (result.size() >= maxMessages) break;
            if (tokenCount + msg.getEstimatedTokens() > maxTokens) break;
            result.add(msg);
            tokenCount += msg.getEstimatedTokens();
        }

        Collections.reverse(result); // восстанавливаем хронологический порядок
        return Collections.unmodifiableList(result);
    }

    /**
     * Форматирует историю диалога для вставки в промпт LLM.
     */
    public String formatHistoryForPrompt() {
        List<Message> messages = getHistoryForPrompt();
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("ИСТОРИЯ ДИАЛОГА:\n");
        for (Message m : messages) {
            sb.append(m).append("\n");
        }
        return sb.toString();
    }

    /**
     * Очистить историю (например, при смене документа или явном reset).
     */
    public void clear() {
        history.clear();
    }

    /**
     * Обрезает историю по лимиту maxMessages (токенный лимит проверяется при чтении).
     */
    private void trim() {
        while (history.size() > maxMessages * 2) { // *2: храним с запасом, обрезаем при чтении
            history.remove(0);
        }
    }
}
