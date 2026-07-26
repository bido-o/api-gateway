package com.bido.api_gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Denylist de suspendare, citit din Redis pe fiecare cerere protejată.
 *
 * Cheile ({@code suspended:{userId}}) sunt scrise de Auth Service în momentul
 * suspendării, cu un TTL egal cu durata access token-ului. Asta permite revocare
 * instant: token-ul rămâne valid ca semnătură, dar gateway-ul îl respinge imediat.
 */
@Slf4j
@Component
public class SuspensionDenylist {

    /** Codul întors clientului când token-ul aparține unui cont suspendat. */
    public static final String SUSPENDED_CODE = "ACCOUNT_SUSPENDED";

    private static final String KEY_PREFIX = "suspended:";

    private final ReactiveStringRedisTemplate redis;

    public SuspensionDenylist(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Verifică dacă userul e în denylist.
     *
     * Fail-open: dacă Redis e indisponibil, lasă cererea să treacă. Suspendarea
     * rămâne acoperită de verificarea din Auth Service la refresh (≤ o durată de
     * access token), deci un blip Redis nu blochează toți userii autentificați.
     */
    public Mono<Boolean> isSuspended(String userId) {
        return redis.hasKey(KEY_PREFIX + userId)
                .onErrorResume(e -> {
                    log.error("Denylist suspendare indisponibil (Redis). Se lasă cererea să treacă. Motiv: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
