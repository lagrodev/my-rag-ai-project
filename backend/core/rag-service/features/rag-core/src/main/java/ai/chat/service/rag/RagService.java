package ai.chat.service.rag;

import ai.chat.rest.dto.ChatResponse;
import ai.chat.service.conversation.ConversationContext;

import java.util.UUID;

/**
 * RAG (Retrieval-Augmented Generation) пайплайн.
 * Объединяет Retrieval + Augmentation (промпт) + Generation (LLM).
 */
public interface RagService {

    /**
     * Ответить на вопрос пользователя на основе содержимого документа.
     *
     * @param userQuery   вопрос пользователя
     * @param fileAssetId ID документа, по которому ведётся Q&A
     * @return ответ LLM + список источников
     */
    ChatResponse chat(String userQuery, UUID fileAssetId);

    ChatResponse chatAsUser(ConversationContext context, String userQuery, UUID fileAssetId, UUID userId);
}
