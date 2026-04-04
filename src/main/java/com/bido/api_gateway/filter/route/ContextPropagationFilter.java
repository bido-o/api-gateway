package com.bido.api_gateway.filter.route;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Objects;

public class ContextPropagationFilter extends AbstractGatewayFilterFactory<ContextPropagationFilter.Config> {

    //@Autowired
    public ContextPropagationFilter() {
        super(Config.class);
    }

    //TODO: review apply
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            //log.debug("Execut ContextPropagationFilter...");

            Claims claims = exchange.getAttribute("authenticatedClaims");
            Objects.requireNonNull(claims,
                    "CRITIC: Lipsesc Claims! Verifica dacă JwtValidationFilter este pus corect înaintea acestui filtru în application.yaml!");

            //Long userId = Long.valueOf(claims.getSubject());

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role != null ? role : "USER")
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {}
}
