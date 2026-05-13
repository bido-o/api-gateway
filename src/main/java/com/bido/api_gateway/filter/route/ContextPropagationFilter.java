package com.bido.api_gateway.filter.route;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ContextPropagationFilter extends AbstractGatewayFilterFactory<ContextPropagationFilter.Config> {

    public ContextPropagationFilter() {
        super(Config.class);
    }

    @NonNull
    @Override
    public GatewayFilter apply(@NonNull Config config) {
        return (exchange, chain) -> {

            //Header Stripping
            ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.remove("X-User-Id");
                        h.remove("X-User-Role");
                        h.remove("X-User-Email");
                    })
                    .build();

            ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();
            Claims claims = sanitizedExchange.getAttribute("authenticatedClaims");

            if(claims == null) {
                log.error("ContextPropagation: Lipsesc claims. Posibil ca JwtValidationFilter să nu fi fost rulat.");
                return Mono.error(
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Eroare internă de server. Vă rugăm să încercați din nou."
                ));
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            ServerHttpRequest mutatedRequest = sanitizedRequest
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            log.debug("Headere propagate pt UserID: {} | Rol: {}", userId, role);

            return chain.filter(sanitizedExchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {}
}
