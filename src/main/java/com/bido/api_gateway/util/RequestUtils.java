package com.bido.api_gateway.util;

import org.springframework.web.server.ServerWebExchange;

import java.util.Objects;

public final class RequestUtils {

    private RequestUtils() {}

    public static String extractIp(ServerWebExchange exchange) {
        try {
            return Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                    .getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String extractPath(ServerWebExchange exchange) {
        return exchange.getRequest().getURI().getPath();
    }

    public static String extractMethod(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod().name();
    }
}
