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
            Claims claims = exchange.getAttribute("authenticatedClaims");

            if(claims == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token lipsă sau invalid");
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            if (userId == null || userId.trim().isEmpty()) {
                log.error("Token fără Subject (User ID)! Verifică Auth Service.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autentificare invalidă. Lipsește identitatea.");
            }

            if (role == null || role.trim().isEmpty()) {
                log.warn("Token validat, dar rolul lipseste pentru UserID: {}", userId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interzis. Rol lipsă.");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            log.debug("Headere propagate pt UserID: {} | Rol: {}", userId, role);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {}
}
