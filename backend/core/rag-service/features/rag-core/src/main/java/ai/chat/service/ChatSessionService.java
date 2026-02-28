package ai.chat.service;

import ai.chat.service.conversation.ConversationContext;

import java.util.UUID;

public interface ChatSessionService {

    /**
     * Пытаемся достать историю диалога из Редиса.
     * Если юзер пишет впервые (или история удалилась по времени) — создаем новую.
     */
    ConversationContext getOrCreateContext(UUID userId, UUID fileAssetId);


    /**
     * Сохраняем обновленную историю обратно в Редис.
     */
    void saveContext(ConversationContext context);




}
