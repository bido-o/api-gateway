package com.bido.api_gateway.exception;

import com.bido.api_gateway.util.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtErrorHandler {

    public Mono<Void> handleJwtException(ServerWebExchange exchange, Exception e) {
        String clientIP = RequestUtils.extractIp(exchange);

        HttpStatus httpStatus;
        String errorMessage;
        String code = null;

        if(e instanceof JwtAuthenticationException jwtException){
            httpStatus = jwtException.getHttpStatus();
            errorMessage = jwtException.getMessage();
            code = jwtException.getCode();
        } else {
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = "Eroare de autentificare.";
        }

        log.warn("Securitate: IP [{}] a eșuat autentificarea. Motiv: {}", clientIP, errorMessage);

        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // `code` apare doar când există (ex. ACCOUNT_SUSPENDED), ca frontend-ul să
        // poată distinge suspendarea de alte erori cu același status.
        String codeField = (code != null) ? String.format("\"code\":\"%s\",", code) : "";
        String jsonFormat = String.format(
                "{%s\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                codeField,
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                errorMessage
        );

        byte[] bytes = jsonFormat.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}