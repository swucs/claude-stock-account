package com.stock.account.broker.kis;

import com.stock.account.broker.dto.BrokerTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KisTokenCacheTest {

    private KisTokenCache cache;

    @BeforeEach
    void setUp() {
        cache = new KisTokenCache();
    }

    @Test
    @DisplayName("토큰 저장 및 조회")
    void putAndGetToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "test-token", "Bearer", LocalDateTime.now().plusHours(24));

        cache.putToken(1L, token);

        BrokerTokenResponse result = cache.getToken(1L);
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("존재하지 않는 계좌 토큰 조회 - null 반환")
    void getTokenNotExists() {
        assertThat(cache.getToken(999L)).isNull();
    }

    @Test
    @DisplayName("만료된 토큰 조회 - null 반환")
    void getExpiredToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "expired-token", "Bearer", LocalDateTime.now().minusHours(1));

        cache.putToken(1L, token);

        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("만료 5분 이내 토큰 조회 - null 반환 (갱신 유도)")
    void getAlmostExpiredToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "almost-expired", "Bearer", LocalDateTime.now().plusMinutes(3));

        cache.putToken(1L, token);

        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("토큰 삭제")
    void removeToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "to-remove", "Bearer", LocalDateTime.now().plusHours(24));

        cache.putToken(1L, token);
        cache.removeToken(1L);

        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("만료된 캐시 정리")
    void clearExpired() {
        cache.putToken(1L, new BrokerTokenResponse(
                "valid", "Bearer", LocalDateTime.now().plusHours(24)));
        cache.putToken(2L, new BrokerTokenResponse(
                "expired", "Bearer", LocalDateTime.now().minusHours(1)));

        cache.clearExpired();

        assertThat(cache.getToken(1L)).isNotNull();
        // 만료된 토큰은 이미 getToken에서 null이지만, clearExpired가 내부적으로 제거
    }
}
