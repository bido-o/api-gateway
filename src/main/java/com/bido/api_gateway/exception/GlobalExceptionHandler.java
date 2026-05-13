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

        String internalErrorMessage = (error != null && error.getMessage() != null)
                                    ? error.getMessage()
                                    : "Eroare fără mesaj";

        Map<String, Object> errorPropertiesMap = getErrorAttributes(serverRequest, ErrorAttributeOptions.defaults());
        HttpStatus status = determineHttpStatus(error);
        String exceptionType = (error != null)
                                ? error.getClass().getSimpleName()
                                : "UnknownException";

        errorPropertiesMap.put("status", status.value());
        errorPropertiesMap.put("error", status.getReasonPhrase());
        errorPropertiesMap.remove("requestId");

        String clientMessage = status.is5xxServerError()
                ? handle5xxError(status, serverRequest.path(), internalErrorMessage, error, exceptionType)
                : handle4xxError(status, serverRequest.path(), internalErrorMessage, error, exceptionType);

        errorPropertiesMap.put("message", clientMessage);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if(error instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }

        if (error instanceof ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE; // 503 (Serviciul e închis/oprit)
        }

        if (error instanceof TimeoutException || error instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT; // 504(Serviciul se mișcă prea greu)
        }

        return HttpStatus.INTERNAL_SERVER_ERROR; //500
    }

    private String handle5xxError(HttpStatus status, String path, String internalErrorMessage, Throwable error, String exceptionType) {
        if(status == HttpStatus.SERVICE_UNAVAILABLE) {
            log.error("SERVICIU INDISPONIBIL [{}] la ruta {} | {}: {}",status.value(), path,exceptionType, internalErrorMessage);
            return "Serviciul este momentan indisponibil. Vă rugăm să încercați din nou mai târziu.";
        }

        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            log.error("GATEWAY TIMEOUT [{}] la ruta {} | {}: {}", status.value(), path, exceptionType, internalErrorMessage);
            return "Serviciul răspunde prea lent. Vă rugăm să încercați din nou mai târziu.";
        }

        log.error("SERVER ERROR [{}] la ruta {} | {}: {}", status.value(), path, exceptionType, internalErrorMessage, error);
        return "Eroare internă de server.";
    }

    private String handle4xxError(HttpStatus status, String path, String internalErrorMessage, Throwable error, String exceptionType) {
        log.warn("CLIENT ERROR [{}] la ruta {} | {}: {}", status.value(), path, exceptionType, internalErrorMessage);

        return (error instanceof ResponseStatusException)
                ? internalErrorMessage
                :"Eroare la procesarea cererii.";
    }
}
