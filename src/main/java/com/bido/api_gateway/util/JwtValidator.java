package com.bido.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import static io.jsonwebtoken.io.Decoders.BASE64;

@Slf4j
@Component
public class JwtValidator {

    private final JwtParser jwtParser;

    public JwtValidator(@Value("${jwt.secret}") String secretKeyString) {
        byte[] keyBytes = BASE64.decode(secretKeyString);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtValidator a fost inițializat și cheia secretă a fost încărcată în memorie.");

        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    private Jws<Claims> validateToken(final String token) {
        return this.jwtParser.parseSignedClaims(token);
    }

    public Claims extractAllClaims(final String token) {
        return validateToken(token).getPayload();
    }

}
