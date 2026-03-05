package com.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Component
@Slf4j
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {
  private final ReactiveRedisTemplate<String,String>redisTemplate;
  @Value("${rate-limit.default-limit:100}")
  private int defaultLimit;
  @Value("${rate-limit.default-window:60}")
  private int defaultWindow;
    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = getClientId(exchange);
            String key = "rate:limit:" + clientId + ":" + exchange.getRequest().getURI().getPath();

            return redisTemplate.opsForValue().increment(key)
                    .flatMap(count -> {
                        if (count == 1) {
                            return redisTemplate.expire(key, Duration.ofSeconds(config.getWindow()))
                                    .thenReturn(count);
                        }
                        return Mono.just(count);
                    })
                    .flatMap(count -> {
                        if (count > config.getLimit()) {
                            log.warn("Rate limit exceeded for client: {}", clientId);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                                    String.valueOf(config.getLimit()));
                            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                            exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
                                    String.valueOf(config.getWindow()));
                            return exchange.getResponse().setComplete();
                        }

                        // Add rate limit headers
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                                String.valueOf(config.getLimit()));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                                String.valueOf(config.getLimit() - count));

                        return chain.filter(exchange);
                    });
        };
    }

    private String getClientId(ServerWebExchange exchange){
        String userId = exchange.getRequest().getHeaders().getFirst("X-User_ID");
        if(userId!=null){
            return "user: "+userId;
        }
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return "ip:" +ip;
    }

    public static class Config{
    private int limit = 100;
    private  int window=60;
    public int getLimit() { return limit;}
        public void setLimit(int limit){this.limit=limit;}
        public int getWindow() {return window;}
        public void setWindow(int window) {this.window=window;}
 }
}