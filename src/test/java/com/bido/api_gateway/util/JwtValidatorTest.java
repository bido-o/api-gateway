package com.bido.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtValidatorTest {
    private static final byte[] SECRET_BYTES = "01234567890123456789012345678901".getBytes();
    private static final String SECRET_BASE64 = Encoders.BASE64.encode(SECRET_BYTES);
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_BYTES);

    private static final byte[] OTHER_SECRET_BYTES = "abcdefghijklmnopqrstuvwxyz123456".getBytes();
    private static final SecretKey OTHER_KEY = Keys.hmacShaKeyFor(OTHER_SECRET_BYTES);

    private static final JwtValidator jwtValidator = new JwtValidator(SECRET_BASE64);

    @Test
    void validToken_returnClaims() {
        String token = Jwts.builder()
                .subject("user-123")
                .claim("role", "USER")
                .claim("email", "test@example.com")
                .signWith(SECRET_KEY)
                .compact();

        Claims claims = jwtValidator.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    }

    @Test
    void expiredToken_throwsExpiredJwtException() {
        String token = Jwts.builder()
                .subject("user-123")
                .claim("role", "USER")
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(SECRET_KEY)
                .compact();

        assertThatThrownBy(() -> jwtValidator.extractAllClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void wrongSignature_throwsSignatureException() {
        String token = Jwts.builder()
                .subject("user-123")
                .claim("role", "USER")
                .signWith(OTHER_KEY)
                .compact();

        assertThatThrownBy(() -> jwtValidator.extractAllClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void malformedToken_throwsMalformedJwtException() {
        assertThatThrownBy(() -> jwtValidator.extractAllClaims("not.a.valid.jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void missingSubject_throwsResponseStatusException500() {
        String token = Jwts.builder()
                .claim("role", "USER")
                .signWith(SECRET_KEY)
                .compact();

        assertThatThrownBy(() -> jwtValidator.extractAllClaims(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void missingRole_throwsResponseStatusException500() {
        String token = Jwts.builder()
                .subject("user-123")
                .signWith(SECRET_KEY)
                .compact();

        assertThatThrownBy(() -> jwtValidator.extractAllClaims(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
