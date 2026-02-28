package ai.chat.backend.logging_common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import jakarta.servlet.DispatcherType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import ai.chat.backend.logging_common.filter.MdcContextFilter;
import ai.chat.backend.logging_common.filter.RequestLoggingFilter;

import java.util.EnumSet;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletLoggingConfiguration {

    @Bean
    @ConditionalOnMissingBean(MdcContextFilter.class)
    @ConditionalOnProperty(name = "app.logging.mdc-context", matchIfMissing = true)
    public FilterRegistrationBean<MdcContextFilter> mdcContextFilter() {
        var registration = new FilterRegistrationBean<>(new MdcContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(RequestLoggingFilter.class)
    @ConditionalOnProperty(name = "app.logging.request-logging", matchIfMissing = true)
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(
            LoggingProperties props, ObjectProvider<MeterRegistry> meterRegistry) {
        var filter = new RequestLoggingFilter(
                props.getSlowRequestThresholdMs(),
                props.getMaxBodyLogSize(),
                meterRegistry.getIfAvailable());
        var registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setDispatcherTypes(
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
        registration.addUrlPatterns("/*");
        return registration;
    }
}
