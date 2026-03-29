package com.bido.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;


@Component
public class JwtValidator {

    private final SecretKey secretKey;

    public JwtValidator(@Value("${jwt.secret}") String secretString) {
        byte[] keyBytes = Decoders.BASE64.decode(secretString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        //log.info("JwtValidator a fost inițializat și cheia secretă a fost încărcată în memorie.");
    }

    public void validateToken(final String token) {
        Jwts.parser().verifyWith(this.secretKey).build().parseSignedClaims(token);
    }

    public Claims extractAllClaims(final String token) {
        return Jwts.parser().verifyWith(this.secretKey).build().parseSignedClaims(token).getPayload();
    }
}
