package com.bido.api_gateway.filter.route;

import com.bido.api_gateway.util.JwtValidator;
import io.jsonwebtoken.Claims;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {
    private final JwtValidator jwtValidator;
    //private final JwtErrorHandler errorHandler;

    @Autowired
    public JwtValidationFilter(JwtValidator jwtValidator) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
        //this.errorHandler = errorHandler; //TODO: review
    }

    @NonNull
    @Override
    public GatewayFilter apply(@NonNull Config config) {
        return (exchange, chain) -> {
            //log.debug("Execut JwtValidation pentru ruta: ", exchange.getRequest().getURI().getPath());

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if(authHeader == null) {
                //log.warn("Security Alert: Lipseste complet header-ul Authorization");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            if(!authHeader.startsWith("Bearer ")) {
                //log.warn("Security Alert: Header-ul Authorization exista, dar nu respecta formatul 'Bearer ' (cu spatiu dupa)");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // TODO: exception handling
            try {
                Claims claims = jwtValidator.extractAllClaims(token);
                exchange.getAttributes().put("authenticatedClaims", claims);
            } catch (Exception e) {
                //log.error("Token invalid sau expirat: " , e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
                //return errorHandler.handle(exchange, e);
            }

            return chain.filter(exchange);
        };
    }

    //TODO: move method to JwtErrorHandler
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}
