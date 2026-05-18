package com.bido.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Primary
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange ->
                Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(addr -> "ip:" + addr.getAddress().getHostAddress())
                        .defaultIfEmpty("ip:unknown");
    }

    @Bean
    KeyResolver userIdKeyResolver() {
        return exchange ->
                Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                        .map(userId -> "user:" + userId);
    }
}
