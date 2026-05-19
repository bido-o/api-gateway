package com.bido.api_gateway.filter.route;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextPropagationFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private GatewayFilter filter; //filtrul real, nu mock!!

    @BeforeEach
    void setUp() {
        filter = new ContextPropagationFilter().apply(new ContextPropagationFilter.Config());
    }

    private MockServerWebExchange exchangeWithClaims(Claims claims, MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("authenticatedClaims", claims);
        return exchange;
    }

    @Test
    void claimsPresent_setsUserHeadersFromClaims() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-7");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(claims.get("email", String.class)).thenReturn("a@b.com");

        MockServerWebExchange exchange = exchangeWithClaims(
                claims, MockServerHttpRequest.get("/api/x").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        HttpHeaders headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-7");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ADMIN");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("a@b.com");
    }

    @Test
    void claimsMissing_emitsInternalServerError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMatches(t -> t instanceof ResponseStatusException rse
                        && rse.getStatusCode().value() == HttpStatus.INTERNAL_SERVER_ERROR.value())
                .verify();

        verifyNoInteractions(chain);
    }

    @Test
    void emailNullInClaims_setsEmptyEmailHeader() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-7");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("email", String.class)).thenReturn(null);

        MockServerWebExchange exchange = exchangeWithClaims(
                claims, MockServerHttpRequest.get("/api/x").build());

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-User-Email"))
                .isEqualTo("");
    }

    @Test
    void incomingUserHeaders_areStrippedBeforePropagation() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("real-user");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("email", String.class)).thenReturn("real@b.com");

        MockServerHttpRequest spoofed = MockServerHttpRequest.get("/api/x")
                .header("X-User-Id", "spoofed-user")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "spoof@b.com")
                .build();
        MockServerWebExchange exchange = exchangeWithClaims(claims, spoofed);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        HttpHeaders headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.get("X-User-Id")).containsExactly("real-user");
        assertThat(headers.get("X-User-Role")).containsExactly("USER");
        assertThat(headers.get("X-User-Email")).containsExactly("real@b.com");
    }
}