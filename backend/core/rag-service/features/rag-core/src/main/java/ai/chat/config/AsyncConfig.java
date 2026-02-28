package ai.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Включает поддержку @Async для асинхронного выполнения методов.
 * Используется в QueryLogServiceImpl для неблокирующего сохранения логов.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot автоматически использует пул потоков по умолчанию (SimpleAsyncTaskExecutor).
    // но рекомендуется настроить ThreadPoolTaskExecutor явно:
    //
    // @Bean(name = "ragAsyncExecutor")
    // public Executor ragAsyncExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(2);
    //     executor.setMaxPoolSize(5);
    //     executor.setQueueCapacity(100);
    //     executor.setThreadNamePrefix("rag-async-");
    //     executor.initialize();
    //     return executor;
    // }
}
