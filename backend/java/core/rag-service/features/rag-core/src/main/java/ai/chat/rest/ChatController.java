package ai.chat.rest;

import ai.chat.backend.logging_common.aspect.Loggable;
import ai.chat.rest.dto.ChatRequest;
import ai.chat.rest.dto.ChatResponse;
import ai.chat.service.ChatSessionService;
import ai.chat.service.conversation.ConversationContext;
import ai.chat.service.conversation.Message;
import ai.chat.service.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
@Loggable(level = Loggable.LogLevel.DEBUG, slowThresholdMs = 300)
public class ChatController {
    private final ChatSessionService chatSessionService;
    private final RagService ragService;

    @PostMapping("/ask")
    public ChatResponse ask(
            @RequestBody
            ChatRequest chatRequest) {
        ConversationContext context = chatSessionService.getOrCreateContext(chatRequest.userId(),
                chatRequest.fileAssetId());
        context.addMessage(
                Message.Role.USER,
                chatRequest.query()
        );

        ChatResponse response = ragService.chatAsUser(
                context,
                chatRequest.query(),
                chatRequest.fileAssetId(),
                chatRequest.userId()
        );

        context.addMessage(
                Message.Role.ASSISTANT,
                response.answer()
        );

        chatSessionService.saveContext(context);

        return response;

    }

}
