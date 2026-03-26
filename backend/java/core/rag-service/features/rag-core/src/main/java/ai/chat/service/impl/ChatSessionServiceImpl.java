package ai.chat.service.impl;

import ai.chat.config.RagProperties;
import ai.chat.service.ChatSessionService;
import ai.chat.service.conversation.ConversationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final RedisTemplate<String, ConversationContext> redisTemplate;

    // Нужен для создания нового контекста, если в Редисе пусто
    private final RagProperties ragProperties;

    @Override
    public ConversationContext getOrCreateContext(UUID userId, UUID fileAssetId) {
        String builtKey = buildKey(userId, fileAssetId);
        ConversationContext context = redisTemplate.opsForValue().get(builtKey);

        if (context == null) {
            log.info("Creating new context for built key {}", builtKey);
            context = new ConversationContext(fileAssetId, userId, ragProperties);
        } else {
            log.info("Loaded existing context for built key {}", builtKey);
        }

        return context;
    }

    @Override
    public void saveContext(ConversationContext context) {
        String key = buildKey(context.getUserId(), context.getFileAssetId());
        redisTemplate.opsForValue().set(key, context, Duration.ofHours(24)); // Сохраняем с TTL, чтобы не засорять память
        log.debug("Saved context for built key {}", key);

    }

    private String buildKey(UUID userId, UUID fileAssetId) {
        return "chat:session:" + userId + ":" + fileAssetId;
    }
}
