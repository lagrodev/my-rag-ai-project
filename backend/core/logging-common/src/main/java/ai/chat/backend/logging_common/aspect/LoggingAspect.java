package ai.chat.backend.logging_common.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.lang.Nullable;

import java.util.Arrays;


@Slf4j
@Aspect
public class LoggingAspect {

    static final String SLOW_METRIC = "logging.slow_requests.total";

    @Nullable
    private final MeterRegistry meterRegistry;

    public LoggingAspect(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Around("@within(ai.chat.backend.logging_common.aspect.Loggable) "
          + "|| @annotation(ai.chat.backend.logging_common.aspect.Loggable)")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();


        Loggable loggable = sig.getMethod().getAnnotation(Loggable.class);
        if (loggable == null) {
            loggable = (Loggable) sig.getDeclaringType().getAnnotation(Loggable.class);
        }
        if (loggable == null) {

            return pjp.proceed();
        }

        String method = sig.getDeclaringType().getSimpleName() + "#" + sig.getName();


        if (loggable.logArgs()) {
            logAt(loggable.level(), ">> {} args={}", method, Arrays.toString(pjp.getArgs()));
        } else {
            logAt(loggable.level(), ">> {}", method);
        }

        long start = System.currentTimeMillis();
        try {
            Object result  = pjp.proceed();
            long   elapsed = System.currentTimeMillis() - start;


            if (elapsed > loggable.slowThresholdMs()) {
                log.warn("<< {} finished in {}ms [SLOW – threshold={}ms]",
                        method, elapsed, loggable.slowThresholdMs());
                incrementSlowMetric("method", sig.getDeclaringType().getSimpleName(), sig.getName());
            } else if (loggable.logResult()) {
                logAt(loggable.level(), "<< {} result={} ({}ms)", method, result, elapsed);
            } else {
                logAt(loggable.level(), "<< {} ({}ms)", method, elapsed);
            }

            return result;

        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("<< {} threw {} after {}ms: {}",
                    method, t.getClass().getSimpleName(), elapsed, t.getMessage());
            throw t;
        }
    }


    private void logAt(Loggable.LogLevel level, String format, Object... args) {
        switch (level) {
            case TRACE -> log.trace(format, args);
            case DEBUG -> log.debug(format, args);
            case INFO  -> log.info(format, args);
            case WARN  -> log.warn(format, args);
        }
    }

    private void incrementSlowMetric(String type, String className, String methodName) {
        if (meterRegistry != null) {
            meterRegistry.counter(SLOW_METRIC,
                    "type",   type,
                    "class",  className,
                    "method", methodName
            ).increment();
        }
    }
}
