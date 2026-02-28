package ai.chat.config;

import ai.chat.service.conversation.ConversationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ConversationContext> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ConversationContext> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Как переводить ключи? -> Просто в текст.
        template.setKeySerializer(new StringRedisSerializer());

        // Как переводить значения (наш контекст)? -> В формат JSON.
        template.setValueSerializer(GenericJacksonJsonRedisSerializer.builder().build());
        return template;
    }

}
