package ai.chat.backend.logging_common.aspect;

import java.lang.annotation.*;


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    LogLevel level() default LogLevel.DEBUG;

    long slowThresholdMs() default 500;

    boolean logArgs() default true;

    boolean logResult() default false;

    enum LogLevel { TRACE, DEBUG, INFO, WARN }
}
