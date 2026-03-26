package ai.chat.service.conversation;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Одно сообщение в диалоге пользователя с RAG-ассистентом.
 */
@Getter
public class Message {

    public enum Role {
        USER, ASSISTANT
    }

    private final Role role;
    private final String content;
    private final LocalDateTime timestamp;

    /** Примерный подсчёт токенов: 1 токен ≈ 4 символа (для оценки, не точный) */
    private final int estimatedTokens;

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.estimatedTokens = content.length() / 4;
    }

    @Override
    public String toString() {
        return (role == Role.USER ? "Пользователь" : "Ассистент") + ": " + content;
    }
}
