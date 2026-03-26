package ai.chat.backend.logging_common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ai.chat.backend.logging_common.filter.ReactiveMdcContextFilter;
import ai.chat.backend.logging_common.filter.ReactiveRequestLoggingFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(LoggingProperties.class)
public class ReactiveLoggingConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReactiveMdcContextFilter.class)
    @ConditionalOnProperty(name = "app.logging.mdc-context", matchIfMissing = true)
    public ReactiveMdcContextFilter reactiveMdcContextFilter() {
        return new ReactiveMdcContextFilter();
    }

    @Bean
    @ConditionalOnMissingBean(ReactiveRequestLoggingFilter.class)
    @ConditionalOnProperty(name = "app.logging.request-logging", matchIfMissing = true)
    public ReactiveRequestLoggingFilter reactiveRequestLoggingFilter(
            LoggingProperties props,
            ObjectProvider<MeterRegistry> meterRegistry) {
        return new ReactiveRequestLoggingFilter(
                props.getSlowRequestThresholdMs(),
                meterRegistry.getIfAvailable());
    }
}
