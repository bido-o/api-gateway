//package com.bido.api_gateway.exception;
//
//
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.MalformedJwtException;
//import io.jsonwebtoken.UnsupportedJwtException;
//import io.jsonwebtoken.security.SignatureException;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//
//@Component
//public class JwtErrorHandler {
//
//    public Mono<Void> handle(ServerWebExchange exchange, Exception e) {
//        HttpStatus status = HttpStatus.UNAUTHORIZED;
//        String reason = "Eroare autentificare";
//
//        if (e instanceof ExpiredJwtException) {
//            reason = "Token expirat";
//            //log.info("JWT: {}", reason);
//        } else if (e instanceof SignatureException) {
//            reason = "Semnătură invalidă (Tentativă de fraudă?)";
//            //log.warn("SECURITY ALERT: {}", reason);
//        } else if (e instanceof MalformedJwtException || e instanceof UnsupportedJwtException) {
//            reason = "Format token invalid";
//            //log.error("JWT: {}", reason);
//        } else {
//            status = HttpStatus.INTERNAL_SERVER_ERROR;
//            reason = "Eroare internă server";
//            //log.error("JWT Error critică: {}", e.getMessage());
//        }
//
//        exchange.getResponse().setStatusCode(status);
//        return exchange.getResponse().setComplete();
//    }
//}