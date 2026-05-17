package com.bido.api_gateway.filter.route;

import com.bido.api_gateway.exception.JwtAuthenticationException;
import com.bido.api_gateway.exception.JwtErrorHandler;
import com.bido.api_gateway.util.JwtValidator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtValidationFilterTest {

    @Mock
    private JwtValidator jwtValidator;

    @Mock
    private JwtErrorHandler jwtErrorHandler;

    @Mock
    private GatewayFilterChain chain;

    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtValidationFilter(jwtValidator, jwtErrorHandler)
                .apply(new JwtValidationFilter.Config());
    }

    private ServerWebExchange exchangeNoAuthHeader() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/secure").build());
    }

    private ServerWebExchange exchangeWithAuth(String headerValue) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/secure")
                        .header(HttpHeaders.AUTHORIZATION, headerValue)
                        .build()
        );
    }

    @Test
    void missingAuthorizationHeader_delegatesToErrorHandler() {
        ServerWebExchange exchange = exchangeNoAuthHeader();
        when(jwtErrorHandler.handleJwtException(eq(exchange), any(JwtAuthenticationException.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtErrorHandler).handleJwtException(eq(exchange), any(JwtAuthenticationException.class));
        verifyNoInteractions(chain);
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void nonBearerAuthHeader_delegatesToErrorHandler() {
        ServerWebExchange exchange = exchangeWithAuth("Basic dXNlcjpwYXNz");
        when(jwtErrorHandler.handleJwtException(eq(exchange), any(JwtAuthenticationException.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtErrorHandler).handleJwtException(eq(exchange), any(JwtAuthenticationException.class));
        verifyNoInteractions(chain);
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void emptyTokenAfterBearer_delegatesToErrorHandler() {
        ServerWebExchange exchange = exchangeWithAuth("Bearer ");
        when(jwtErrorHandler.handleJwtException(eq(exchange), any(JwtAuthenticationException.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtErrorHandler).handleJwtException(eq(exchange), any(JwtAuthenticationException.class));
        verifyNoInteractions(chain);
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void whitespaceOnlyToken_delegatesToErrorHandler() {
        ServerWebExchange exchange = exchangeWithAuth("Bearer    ");
        when(jwtErrorHandler.handleJwtException(eq(exchange), any(JwtAuthenticationException.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtErrorHandler).handleJwtException(eq(exchange), any(JwtAuthenticationException.class));
        verifyNoInteractions(chain);
        verifyNoInteractions(jwtValidator);
    }

    @Test
    void validToken_callsChainAndStoresClaimsAttribute() {
        ServerWebExchange exchange = exchangeWithAuth("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-123");
        when(jwtValidator.extractAllClaims("good-token")).thenReturn(claims);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        assertThat(exchange.getAttributes().get("authenticatedClaims")).isSameAs(claims);
        verifyNoInteractions(jwtErrorHandler);
    }

    @Test
    void validatorThrowsResponseStatusException_isPropagated() {
        ServerWebExchange exchange = exchangeWithAuth("Bearer broken-but-signed-ok");
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "missing subject");
        when(jwtValidator.extractAllClaims("broken-but-signed-ok")).thenThrow(rse);

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMatches(t -> t == rse)
                .verify();

        verifyNoInteractions(chain);
        verifyNoInteractions(jwtErrorHandler);
    }

    @Test
    void validatorThrowsGenericException_delegatesToErrorHandlerAs401() {
        ServerWebExchange exchange = exchangeWithAuth("Bearer bad-token");
        when(jwtValidator.extractAllClaims("bad-token"))
                .thenThrow(new RuntimeException("expired"));
        when(jwtErrorHandler.handleJwtException(eq(exchange), any(JwtAuthenticationException.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtErrorHandler).handleJwtException(eq(exchange), any(JwtAuthenticationException.class));
        verifyNoInteractions(chain);
    }
}