package com.stock.account.broker.kis;

import com.stock.account.broker.dto.BrokerTokenResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KisTokenCache {

    private final ConcurrentHashMap<Long, BrokerTokenResponse> cache = new ConcurrentHashMap<>();

    /**
     * 캐시에서 유효한 토큰을 반환한다.
     * 만료 5분 전이면 null을 반환하여 갱신을 유도한다.
     */
    public BrokerTokenResponse getToken(Long accountId) {
        BrokerTokenResponse token = cache.get(accountId);
        if (token == null) {
            return null;
        }
        // 만료 5분 전이면 캐시 무효
        if (token.expiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            cache.remove(accountId);
            return null;
        }
        return token;
    }

    public void putToken(Long accountId, BrokerTokenResponse token) {
        cache.put(accountId, token);
    }

    public void removeToken(Long accountId) {
        cache.remove(accountId);
    }

    public void clearExpired() {
        LocalDateTime now = LocalDateTime.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
