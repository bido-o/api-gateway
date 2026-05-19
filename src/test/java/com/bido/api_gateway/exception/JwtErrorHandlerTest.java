package com.bido.api_gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtErrorHandlerTest {

    private final JwtErrorHandler handler = new JwtErrorHandler();

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/secure").build());
    }

    private String responseBody(MockServerWebExchange exchange) {
        Flux<DataBuffer> body = exchange.getResponse().getBody();
        StringBuilder sb = new StringBuilder();
        body.toIterable().forEach(buf -> {
            byte[] bytes = new byte[buf.readableByteCount()];
            buf.read(bytes);
            sb.append(new String(bytes, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    @Test
    void jwtAuthenticationException_writesStatusAndMessageFromException() {
        MockServerWebExchange exchange = exchange();
        JwtAuthenticationException ex = new JwtAuthenticationException("Sesiune invalida.");

        StepVerifier.create(handler.handleJwtException(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);

        String body = responseBody(exchange);
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"error\":\"Unauthorized\"");
        assertThat(body).contains("\"message\":\"Sesiune invalida.\"");
    }

    @Test
    void genericException_returns401WithGenericMessageAndHidesInternalDetail() {
        MockServerWebExchange exchange = exchange();
        Exception ex = new RuntimeException("internal explosion details");

        StepVerifier.create(handler.handleJwtException(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);

        String body = responseBody(exchange);
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"message\":\"Eroare de autentificare.\"");
        assertThat(body).doesNotContain("internal explosion details");
    }
}