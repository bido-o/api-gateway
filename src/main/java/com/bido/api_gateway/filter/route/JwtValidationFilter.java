package com.bido.api_gateway.filter.route;

import com.bido.api_gateway.exception.JwtAuthenticationException;
import com.bido.api_gateway.exception.JwtErrorHandler;
import com.bido.api_gateway.util.JwtValidator;
import com.bido.api_gateway.util.RequestUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {
    private final JwtValidator jwtValidator;
    private final JwtErrorHandler jwtErrorHandler;

    public JwtValidationFilter(JwtValidator jwtValidator, JwtErrorHandler jwtErrorHandler) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
        this.jwtErrorHandler = jwtErrorHandler;
    }

    @NonNull
    @Override
    public GatewayFilter apply(@NonNull Config config) {
        return (exchange, chain) -> {
            String path = RequestUtils.extractPath(exchange);
            log.debug("Execut JwtValidation pentru ruta: {}", path);

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if(authHeader == null) {
                log.warn("Securitate - Acces refuzat: Lipsește complet header-ul Authorization pe ruta {}", path);
                return jwtErrorHandler.handleJwtException(exchange, new JwtAuthenticationException("Nu sunteți autentificat. Vă rugăm să vă logați pentru a accesa această pagină."));
            }

            if(!authHeader.startsWith("Bearer ")) {
                log.warn("Securitate - Acces refuzat: Format invalid. Header-ul Authorization exista, dar nu respecta formatul 'Bearer ' (cu spatiu dupa) pe ruta {}", path);
                return jwtErrorHandler.handleJwtException(exchange, new JwtAuthenticationException("Sesiune invalidă. Vă rugăm să vă reconectați."));
            }

            String token = authHeader.substring(7).trim();

            if(token.isEmpty()) {
                log.warn("Securitate - Acces refuzat: Token Bearer absent din header-ul Authorization pe ruta {}", path);
                return jwtErrorHandler.handleJwtException(exchange,
                        new JwtAuthenticationException("Nu sunteți autentificat. Vă rugăm să vă logați."));
            }

            try {
                Claims claims = jwtValidator.extractAllClaims(token);
                log.debug("Token validat cu succes pentru UserID: {} pe ruta {}", claims.getSubject(), path);
                exchange.getAttributes().put("authenticatedClaims", claims);
                return chain.filter(exchange);
            } catch (ResponseStatusException e) {
                // 500 — eroare din JwtValidator (bug Auth Service)
                // propagă la GlobalExceptionHandler, nu la JwtErrorHandler
                return Mono.error(e);
            } catch (Exception e) {
                // Excepții jjwt — token expirat, malformat, semnătură greșită
                log.warn("Securitate - Token invalid sau expirat (respins de validator): {}" , e.getMessage());
                return jwtErrorHandler.handleJwtException(exchange,
                        new JwtAuthenticationException("Sesiunea a expirat sau este invalidă. Vă rugăm să vă logați din nou."));
            }
        };
    }

    public static class Config {}
}
