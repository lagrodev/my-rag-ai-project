package ai.chat.backend.logging_common.aspect;

import java.lang.annotation.*;

/**
 * Marks a method <em>or class</em> for automatic entry/exit logging via {@link LoggingAspect}.
 *
 * <p>When placed on a <strong>class</strong>, the annotation applies to all public methods.
 * A method-level {@code @Loggable} always takes precedence over the class-level one,
 * allowing per-method overrides.
 *
 * <p>Example:
 * <pre>{@code
 * @Loggable(level = Loggable.LogLevel.INFO, slowThresholdMs = 300, logArgs = true)
 * public User findById(Long id) { ... }
 * }
 * </pre>
 *
 * <ul>
 *   <li>On entry  – logs class.method + optional argument values.</li>
 *   <li>On exit   – logs duration; promotes to WARN when it exceeds {@link #slowThresholdMs}.</li>
 *   <li>On error  – logs the exception type, message, and duration at ERROR level.</li>
 * </ul>
 */
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
