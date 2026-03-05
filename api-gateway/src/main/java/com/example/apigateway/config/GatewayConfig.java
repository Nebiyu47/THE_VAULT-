package com.example.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(apiKeyResolver()))
                                .removeRequestHeader("Cookie"))
                        .uri("lb://AUTH-SERVICE"))
                .route("lobby-service", r -> r
                        .path("/api/lobby/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("lobbyService")
                                        .setFallbackUri("forward:/fallback/lobby"))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://LOBBY-SERVICE"))
                .route("game-service-rest", r -> r
                        .path("/api/game/**")
                        .and().method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("gameService")
                                        .setFallbackUri("forward:/fallback/game"))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(apiKeyResolver()))
                                .addRequestHeader("X-Request-Start", String.valueOf(System.currentTimeMillis())))
                        .uri("lb://GAME-SERVICE"))
                .route("game-service-ws", r -> r
                        .path("/ws/game/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("gameServiceWs")
                                        .setFallbackUri("forward:/fallback/game")))
                        .uri("lb:ws://GAME-SERVICE"))
                .route("chat-service-rest", r -> r
                        .path("/api/chat/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("chatService")
                                        .setFallbackUri("forward:/fallback/chat"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .addRequestHeader("X-Request-ID", "${uuid}"))
                        .uri("lb://CHAT-SERVICE"))
                .route("payment-service", r -> r
                        .path("/api/payment/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("paymentService")
                                        .setFallbackUri("forward:/fallback/payment"))
                                .retry(config -> config
                                        .setRetries(2)
                                        .setMethods(HttpMethod.GET))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(apiKeyResolver()))
                                .addRequestHeader("X-Transaction-ID", "${uuid}"))
                        .uri("lb://PAYMENT-SERVICE"))
                .route("static-resources", r -> r
                        .path("/static/**")
                        .uri("lb://STATIC-SERVICE"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just(apiKey);
            }
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just(userId);
            }
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }

    @Bean
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            exchange.getAttributes().put("startTime", System.currentTimeMillis());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                Long startTime = exchange.getAttribute("startTime");
                if (startTime != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Request to {} took {} ms",
                            exchange.getRequest().getURI().getPath(), duration);
                }
            }));
        };
    }
}