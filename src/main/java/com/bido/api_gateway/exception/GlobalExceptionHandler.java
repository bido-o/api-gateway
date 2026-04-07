package com.bido.api_gateway.exception;

import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Map;
//import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @NonNull
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(@NonNull ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest serverRequest) {
        Throwable error = getError(serverRequest);
        String safeErrorMessage = (error != null && error.getMessage() != null)
                                    ? error.getMessage()
                                    : "Eroare fără mesaj";

        Map<String, Object> errorPropertiesMap = getErrorAttributes(serverRequest, ErrorAttributeOptions.defaults());
        HttpStatus status = determineHttpStatus(error);

        errorPropertiesMap.put("status", status.value());
        errorPropertiesMap.put("error", status.getReasonPhrase());
        errorPropertiesMap.remove("requestId");

        if (status.is5xxServerError()) {
            log.error("SERVER ERROR [{}] la ruta {}: {}", status.value(), serverRequest.path(), safeErrorMessage, error);
            errorPropertiesMap.put("message", "Eroare internă de server.");
        } else {
            log.warn("CLIENT ERROR [{}] la ruta {}: {}", status.value(), serverRequest.path(), safeErrorMessage);
            errorPropertiesMap.put("message", safeErrorMessage);
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if(error instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }

        if (error instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE; // 503 (Serviciul e închis/oprit)
        }

        if (error instanceof TimeoutException || error instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT; // 504(Serviciul se mișcă prea greu)
        }

        return HttpStatus.INTERNAL_SERVER_ERROR; //500
    }
}
