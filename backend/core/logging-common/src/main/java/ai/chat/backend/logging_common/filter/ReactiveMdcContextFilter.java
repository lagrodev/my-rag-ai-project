package ai.chat.backend.logging_common.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Reactive {@link WebFilter} that propagates MDC context for every incoming request.
 *
 * <p>Sets the following keys in the Reactor {@link Context} (accessible to subscribers)
 * and in SLF4J MDC (for synchronous log calls made in the same thread):
 * <ul>
 *   <li>{@code requestId} – taken from the {@code X-Request-ID} header, or generated as UUID</li>
 * </ul>
 *
 * <p>The {@code X-Request-ID} is echoed back in the response header so clients
 * can correlate their requests with server-side logs.
 *
 * <p>Note: for full MDC propagation across reactive thread hops, configure Reactor's
 * context-propagation support ({@code reactor.core.publisher.Hooks.enableAutomaticContextPropagation()}).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveMdcContextFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID    = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, finalRequestId);

        return chain.filter(exchange)
                .contextWrite(Context.of(MDC_REQUEST_ID, finalRequestId))
                .doOnSubscribe(sub -> MDC.put(MDC_REQUEST_ID, finalRequestId))
                .doFinally(signal -> MDC.remove(MDC_REQUEST_ID));
    }
}
