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
