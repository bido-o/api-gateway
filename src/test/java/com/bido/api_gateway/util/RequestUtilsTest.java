package com.bido.api_gateway.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilsTest {

    @Test
    void extractIp_validRemoteAddress_returnsHostAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x")
                .remoteAddress(new InetSocketAddress("192.168.1.10", 54321))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(RequestUtils.extractIp(exchange)).isEqualTo("192.168.1.10");
    }

    @Test
    void extractIp_nullRemoteAddress_returnsUnknown() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(RequestUtils.extractIp(exchange)).isEqualTo("unknown");
    }

    @Test
    void extractPath_returnsRequestUriPath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/42").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(RequestUtils.extractPath(exchange)).isEqualTo("/api/v1/users/42");
    }

    @Test
    void extractMethod_returnsHttpMethodName() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/x").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(RequestUtils.extractMethod(exchange)).isEqualTo("POST");
    }
}