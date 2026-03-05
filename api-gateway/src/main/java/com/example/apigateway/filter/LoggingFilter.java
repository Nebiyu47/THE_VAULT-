package com.example.apigateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate and store request metadata
        String requestId = UUID.randomUUID().toString();
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", System.currentTimeMillis());

        // FIX: Extract headers safely without the 'entrySet' resolution issue
        Map<String, String> filteredHeaders = request.getHeaders().toSingleValueMap().entrySet().stream()
                .filter(e -> !e.getKey().equalsIgnoreCase("Authorization"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Log request - Ensure arguments match the placeholders
        log.info("Request [{}] - Method: {}, Path: {}, Headers: {}",
                requestId,
                request.getMethod(),
                request.getURI().getPath(),
                filteredHeaders);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Log response
            Long startTime = exchange.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Response [{}] - Status: {}, Duration: {}ms",
                        requestId,
                        exchange.getResponse().getStatusCode(),
                        duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        // High priority so it captures the very start and very end
        return Ordered.HIGHEST_PRECEDENCE;
    }
}