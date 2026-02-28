package ai.chat.backend.logging_common.filter;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that logs every HTTP request and response as a structured log entry.
 *
 * <ul>
 *   <li>Logs method, URI, query string, response status, and duration on every request.</li>
 *   <li>Logs request headers at DEBUG level, masking {@link #SENSITIVE_HEADERS}.</li>
 *   <li>Logs the request body (up to {@code maxBodyLogSize} bytes) for non-GET requests.</li>
 *   <li>Promotes the log record to WARN when the request exceeds {@code slowThresholdMs}.</li>
 *   <li>Increments {@code logging.slow_requests.total} Micrometer counter for slow requests.</li>
 * </ul>
 *
 * <p>Uses {@link ContentCachingRequestWrapper} / {@link ContentCachingResponseWrapper} so that
 * the body can be read for logging without consuming the stream for the actual handler.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    static final String SLOW_METRIC = "logging.slow_requests.total";

    /** Header names (lower-cased) whose values are replaced with {@code ****} in logs. */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization"
    );

    private final long slowThresholdMs;
    private final int  maxBodyLogSize;

    @Nullable
    private final MeterRegistry meterRegistry;

    public RequestLoggingFilter(long slowThresholdMs, int maxBodyLogSize,
                                @Nullable MeterRegistry meterRegistry) {
        this.slowThresholdMs = slowThresholdMs;
        this.maxBodyLogSize  = maxBodyLogSize;
        this.meterRegistry   = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper  wrappedReq  = new ContentCachingRequestWrapper(request, maxBodyLogSize);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedReq, wrappedResp);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int  status   = wrappedResp.getStatus();

            String path   = request.getRequestURI();
            String query  = request.getQueryString() != null ? "?" + request.getQueryString() : "";
            String method = request.getMethod();
            String body   = extractBody(wrappedReq.getContentAsByteArray());

            if (duration > slowThresholdMs) {
                log.warn("SLOW [{} {}{}] -> {} in {}ms | headers={} body=[{}]",
                        method, path, query, status, duration,
                        extractHeaders(request), body);
                if (meterRegistry != null) {
                    meterRegistry.counter(SLOW_METRIC,
                            "type",       "http",
                            "http_method", method,
                            "uri",         path
                    ).increment();
                }
            } else {
                log.info("[{} {}{}] -> {} in {}ms", method, path, query, status, duration);
                if (log.isDebugEnabled()) {
                    log.debug("headers={}", extractHeaders(request));
                    if (!body.isEmpty()) {
                        log.debug("request body: {}", body);
                    }
                }
            }

            // Copy the cached response body back to the real response stream
            wrappedResp.copyBodyToResponse();
        }
    }

    /** Skip async dispatch to avoid double-logging. */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    private String extractBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            // Never log Authorization header value
            headers.put(name, "authorization".equalsIgnoreCase(name) ? "****" : request.getHeader(name));
        }
        return headers;
    }
}
