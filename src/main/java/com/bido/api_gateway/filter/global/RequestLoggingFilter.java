package com.bido.api_gateway.filter.global;

import com.bido.api_gateway.util.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;


@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestLoggingFilter implements GlobalFilter {

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String method = RequestUtils.extractMethod(exchange);
        String path = RequestUtils.extractPath(exchange);
        String ip = RequestUtils.extractIp(exchange);
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            String logMessage = "{} {} | IP: {} | {} | {}ms";

            if(signalType == SignalType.CANCEL) {
                log.warn(logMessage, method, path, ip, "CANCELLED", duration);
                return;
            }

            HttpStatusCode httpStatus = exchange.getResponse().getStatusCode();
            if(httpStatus == null) {
                log.warn(logMessage, method, path, ip, "NO_STATUS", duration);
                return;
            }
            int statusCode = httpStatus.value();

            Level level = (statusCode >= 500) ? Level.ERROR : (statusCode >= 400) ? Level.WARN : Level.INFO;
            log.atLevel(level).log(logMessage, method, path, ip, statusCode, duration);
        });
    }
}
