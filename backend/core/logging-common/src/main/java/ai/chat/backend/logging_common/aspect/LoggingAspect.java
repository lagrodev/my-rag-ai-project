package ai.chat.backend.logging_common.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.lang.Nullable;

import java.util.Arrays;

/**
 * AOP aspect backing the {@link Loggable} annotation.
 *
 * <p>Registered automatically by {@code LoggingAutoConfiguration}.
 * Can be disabled per-service with {@code app.logging.loggable-aspect=false}.
 *
 * <p>Supports both method-level and class-level {@code @Loggable}.
 * Method-level always takes precedence over class-level for attribute resolution.
 *
 * <p>When a {@link MeterRegistry} is present in the context, increments the
 * {@code logging.slow_requests.total} counter for every slow method call.
 */
@Slf4j
@Aspect
public class LoggingAspect {

    static final String SLOW_METRIC = "logging.slow_requests.total";

    @Nullable
    private final MeterRegistry meterRegistry;

    public LoggingAspect(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Intercepts methods annotated directly with {@code @Loggable} OR methods
     * whose declaring class is annotated with {@code @Loggable}.
     */
    @Around("@within(ai.chat.backend.logging_common.aspect.Loggable) "
          + "|| @annotation(ai.chat.backend.logging_common.aspect.Loggable)")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();

        // Method-level annotation takes precedence; fall back to class-level
        Loggable loggable = sig.getMethod().getAnnotation(Loggable.class);
        if (loggable == null) {
            loggable = (Loggable) sig.getDeclaringType().getAnnotation(Loggable.class);
        }
        if (loggable == null) {
            // shouldn't happen given the pointcut, but guard against it
            return pjp.proceed();
        }

        String method = sig.getDeclaringType().getSimpleName() + "#" + sig.getName();

        // ── Entry log ────────────────────────────────────────────────────────────
        if (loggable.logArgs()) {
            logAt(loggable.level(), ">> {} args={}", method, Arrays.toString(pjp.getArgs()));
        } else {
            logAt(loggable.level(), ">> {}", method);
        }

        long start = System.currentTimeMillis();
        try {
            Object result  = pjp.proceed();
            long   elapsed = System.currentTimeMillis() - start;

            // ── Exit log ──────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────

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
