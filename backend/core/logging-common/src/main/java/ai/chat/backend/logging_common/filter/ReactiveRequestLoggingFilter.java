package ai.chat.backend.logging_common.filter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ReactiveRequestLoggingFilter implements WebFilter {

    static final String SLOW_METRIC = "logging.slow_requests.total";


    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization"
    );

    private final long slowThresholdMs;

    @Nullable
    private final MeterRegistry meterRegistry;

    public ReactiveRequestLoggingFilter(long slowThresholdMs, @Nullable MeterRegistry meterRegistry) {
        this.slowThresholdMs = slowThresholdMs;
        this.meterRegistry   = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long   start  = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path   = exchange.getRequest().getURI().getPath();
        String query  = exchange.getRequest().getURI().getQuery() != null
                        ? "?" + exchange.getRequest().getURI().getQuery() : "";

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    int  status   = exchange.getResponse().getStatusCode() != null
                                    ? exchange.getResponse().getStatusCode().value() : 0;

                    if (duration > slowThresholdMs) {
                        log.warn("SLOW [{} {}{}] -> {} in {}ms | headers={}",
                                method, path, query, status, duration,
                                maskedHeaders(exchange));
                        if (meterRegistry != null) {
                            meterRegistry.counter(SLOW_METRIC,
                                    "type",        "http",
                                    "http_method", method,
                                    "uri",         path
                            ).increment();
                        }
                    } else {
                        log.info("[{} {}{}] -> {} in {}ms", method, path, query, status, duration);
                        if (log.isDebugEnabled()) {
                            log.debug("headers={}", maskedHeaders(exchange));
                        }
                    }
                });
    }

    private Map<String, String> maskedHeaders(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().headerSet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> SENSITIVE_HEADERS.contains(e.getKey().toLowerCase())
                                ? "****"
                                : String.join(", ", e.getValue()),
                        (a, b) -> a
                ));
    }
}
