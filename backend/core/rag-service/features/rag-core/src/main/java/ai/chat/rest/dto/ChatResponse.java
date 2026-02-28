package ai.chat.rest.dto;

import lombok.Builder;

import java.util.List;

/**
 * Ответ RAG пайплайна на вопрос пользователя.
 */
@Builder
public record ChatResponse(
        /** Ответ, сгенерированный LLM на основе найденного контекста */
        String answer,
        List<SourceReference> sources
) {

}
