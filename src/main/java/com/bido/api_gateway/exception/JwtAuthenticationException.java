package com.bido.api_gateway.exception;

import org.springframework.http.HttpStatus;

public class JwtAuthenticationException extends RuntimeException{
    private final HttpStatus httpStatus;

    public JwtAuthenticationException(String message) {
        super(message);
        this.httpStatus = HttpStatus.UNAUTHORIZED; //default 401
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
