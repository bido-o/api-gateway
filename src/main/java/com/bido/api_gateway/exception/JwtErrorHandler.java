package com.bido.api_gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
public class JwtErrorHandler {

    public Mono<Void> handleJwtException(ServerWebExchange exchange, Exception e) {
        String clientIP = getClientIP(exchange);

        HttpStatus httpStatus;
        String errorMessage;

        if(e instanceof JwtAuthenticationException jwtException){
            httpStatus = jwtException.getHttpStatus();
            errorMessage = jwtException.getMessage();
        } else {
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = "Eroare de autentificare.";
        }

        log.warn("Securitate: IP [{}] a eșuat autentificarea. Motiv: {}", clientIP, errorMessage);

        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonFormat = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                errorMessage
        );

        byte[] bytes = jsonFormat.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String getClientIP(ServerWebExchange exchange) {
        try {
            return Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
        } catch (Exception e) {
            log.warn("Nu am putut obține adresa IP a clientului: {}", e.getMessage());
            return "IP necunoscut";
        }
    }
}