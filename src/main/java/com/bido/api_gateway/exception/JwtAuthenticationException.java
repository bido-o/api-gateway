package com.bido.api_gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JwtAuthenticationException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String code;

    public JwtAuthenticationException(String message) {
        this(HttpStatus.UNAUTHORIZED, null, message); // default 401, fără cod
    }

    public JwtAuthenticationException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

}
