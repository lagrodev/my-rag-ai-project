package ai.chat.service.rag.impl;

import ai.chat.config.RagProperties;
import ai.chat.service.conversation.ConversationContext;
import ai.chat.service.llm.LlmClient;
import ai.chat.service.logging.QueryLogService;
import ai.chat.service.rag.RagService;
import ai.chat.rest.dto.ChatResponse;
import ai.chat.rest.dto.SourceReference;
import ai.chat.service.retrieval.RetrievalService;
import ai.chat.service.retrieval.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Реализация полного RAG пайплайна:
 * [Retrieval] → [Context Assembly] → [Prompt Building] → [LLM] → [Response]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final RetrievalService retrievalService;
    private final LlmClient llmClient;
    private final RagProperties ragProperties;
    private final QueryLogService queryLogService;

    @Override
    public ChatResponse chat(String userQuery, UUID fileAssetId) {
        return chatAsUser(null, userQuery, fileAssetId, null);
    }

    /**
     * Chat с указанием userId для логирования.
     */
    @Override
    public ChatResponse chatAsUser(ConversationContext conversation, String userQuery, UUID fileAssetId, UUID userId) {
        log.info("RAG chat: fileAssetId={}, userId={}, query='{}'", fileAssetId, userId, userQuery);
        long startMs = System.currentTimeMillis();

        List<RetrievedChunk> relevantChunks = List.of();

        // Retrieval — находим релевантные фрагменты
        try {
            int topK = ragProperties.getRetrieval().getDefaultTopK();
            relevantChunks = retrievalService.retrieve(userQuery, fileAssetId, topK);
        } catch (Exception e) {
            logError(
                    "Retrieval",
                    startMs,
                    fileAssetId, userId, userQuery, e.getMessage(), e
            );
            throw new RuntimeException("Ошибка поиска релевантных документов", e);
        }

        if (relevantChunks.isEmpty()) {
            log.warn("Retrieval вернул 0 результатов для fileAssetId={}, query='{}'", fileAssetId, userQuery);
            ChatResponse emptyResponse = ChatResponse.builder()
                    .answer("Не удалось найти в документе информацию, относящуюся к вашему вопросу.")
                    .sources(List.of())
                    .build();
            queryLogService.logSuccess(fileAssetId, userId, userQuery, emptyResponse, List.of(),
                    System.currentTimeMillis() - startMs);
            return emptyResponse;
        }

        // Сборка контекста — объединяем фрагменты, ограничиваем по maxContextLength
        String context = buildContext(relevantChunks);

        // Построение промпта
        String prompt = buildPrompt(userQuery, context, conversation);
        log.debug("Промпт для LLM (длина {}), первые 200 символов: {}",
                prompt.length(), prompt.substring(0, Math.min(prompt.length(), 200)));

        // Отправляем в LLM
        String answer;
        try {
            answer = llmClient.complete(prompt);
        } catch (Exception e) {
            logError(
                    "LLM",
                    startMs,
                    fileAssetId, userId, userQuery, e.getMessage(), e
            );
            throw new RuntimeException("Ошибка генерации ответа LLM", e);
        }

        // Формируем ответ с источниками
        List<SourceReference> sources = relevantChunks.stream()
                .map(SourceReference::from)
                .toList();

        ChatResponse response = ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .build();

        long latency = System.currentTimeMillis() - startMs;
        log.info("RAG ответ сформирован: fileAssetId={}, источников={}, latencyMs={}, длина ответа={}",
                fileAssetId, sources.size(), latency, answer.length());

        // Шаг 6: Логируем асинхронно (не блокирует ответ)
        queryLogService.logSuccess(fileAssetId, userId, userQuery, response, relevantChunks, latency);

        return response;
    }

    private void logError(
            String stage,
            long startMs, UUID fileAssetId, UUID userId, String userQuery, String errorMessage,
            Exception e
    ){
        long latency = System.currentTimeMillis() - startMs;
        log.error("Ошибка на этапе {} для fileAssetId={}, query='{}': {}", stage, fileAssetId, userQuery, errorMessage, e);
        queryLogService.logError(fileAssetId, userId, userQuery, errorMessage, latency);
    }

    /**
     * Объединяет фрагменты в строку контекста.
     * Ограничивает общую длину через rag.llm.max-context-length.
     */
    private String buildContext(List<RetrievedChunk> chunks) {
        int maxLen = ragProperties.getLlm().getMaxContextLength();
        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            String chunkText = formatChunkWithSource(chunk);
            int separatorLength = sb.isEmpty() ? 0 : 9;
            if (sb.length() + separatorLength + chunkText.length() > maxLen) {
                log.debug("Контекст обрезан по maxContextLength={}: добавлено {} из {} фрагментов",
                        maxLen, chunks.indexOf(chunk), chunks.size());
                break;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n---\n\n");
            }

            sb.append(chunkText);
        }
        return sb.toString();
    }


    private String formatChunkWithSource(RetrievedChunk chunk) {
        StringBuilder sb = new StringBuilder();
        if (chunk.headingTitle() != null && !chunk.headingTitle().isBlank()) {
            sb.append("[").append(chunk.headingTitle()).append("]");
            if (chunk.pageNumber() != null) {
                sb.append(" (стр. ").append(chunk.pageNumber()).append(")");
            }
            sb.append("\n");
        }
        sb.append(chunk.contextText());
        return sb.toString();
    }


    private String buildPrompt(String query, String documentsContext, ConversationContext conversation) {

        //  история
        String historyText = (conversation != null) ? conversation.formatHistoryForPrompt() : "";

        // шаблон
        return """
            Ты корпоративный помощник по работе с документами.
            Отвечай ТОЛЬКО на основе предоставленного НАЙДЕННОГО КОНТЕКСТА.
            Учитывай историю диалога для понимания контекста вопроса, но факты бери только из документов.
            Если ответа нет в документах — скажи об этом прямо, не придумывай.

            НАЙДЕННЫЙ КОНТЕКСТ:
            %s

            %s

            ТЕКУЩИЙ ВОПРОС: %s

            ОТВЕТ:
            """.formatted(
                documentsContext,
                historyText,
                query
        );
    }
}
