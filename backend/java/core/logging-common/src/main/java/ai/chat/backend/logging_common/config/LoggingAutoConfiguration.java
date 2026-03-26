package ai.chat.backend.logging_common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import io.micrometer.core.instrument.MeterRegistry;
import ai.chat.backend.logging_common.aspect.LoggingAspect;
import ai.chat.backend.logging_common.async.MdcTaskDecorator;

@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoggingAspect loggingAspect(ObjectProvider<MeterRegistry> meterRegistry) {
        return new LoggingAspect(meterRegistry.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
