package com.stock.account.broker.kis;

import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.common.util.AesEncryptionUtil;
import com.stock.account.domain.BrokerToken;
import com.stock.account.domain.BrokerType;
import com.stock.account.repository.BrokerTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisTokenCacheTest {

    @Mock
    private BrokerTokenRepository brokerTokenRepository;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    private KisTokenCache cache;

    @BeforeEach
    void setUp() {
        cache = new KisTokenCache(brokerTokenRepository, aesEncryptionUtil);
    }

    @Test
    @DisplayName("토큰 저장 - 메모리 캐시와 DB에 동시 저장")
    void putAndGetToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "test-token", "Bearer", LocalDateTime.now().plusHours(24));

        when(aesEncryptionUtil.encrypt("test-token")).thenReturn("encrypted-test-token");
        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.empty());

        cache.putToken(1L, token);

        // 메모리 캐시에서 조회 (DB 호출 없이)
        BrokerTokenResponse result = cache.getToken(1L);
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("test-token");

        // DB에 저장되었는지 확인
        verify(brokerTokenRepository).save(any(BrokerToken.class));
    }

    @Test
    @DisplayName("토큰 저장 - 기존 토큰이 있으면 업데이트")
    void putTokenUpdatesExisting() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "new-token", "Bearer", LocalDateTime.now().plusHours(24));

        BrokerToken existingEntity = BrokerToken.builder()
                .accountId(1L)
                .brokerType(BrokerType.KIS)
                .accessToken("old-encrypted")
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusHours(12))
                .build();

        when(aesEncryptionUtil.encrypt("new-token")).thenReturn("encrypted-new-token");
        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.of(existingEntity));

        cache.putToken(1L, token);

        // save가 호출되지 않고 기존 엔티티가 업데이트됨
        verify(brokerTokenRepository, never()).save(any());
        assertThat(existingEntity.getAccessToken()).isEqualTo("encrypted-new-token");
    }

    @Test
    @DisplayName("메모리 캐시 미스 시 DB에서 조회")
    void getTokenFromDbWhenMemoryMiss() {
        BrokerToken dbToken = BrokerToken.builder()
                .accountId(1L)
                .brokerType(BrokerType.KIS)
                .accessToken("encrypted-db-token")
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().plusHours(20))
                .build();

        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.of(dbToken));
        when(aesEncryptionUtil.decrypt("encrypted-db-token")).thenReturn("db-token");

        BrokerTokenResponse result = cache.getToken(1L);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("db-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("존재하지 않는 계좌 토큰 조회 - null 반환")
    void getTokenNotExists() {
        when(brokerTokenRepository.findByAccountIdAndBrokerType(999L, BrokerType.KIS))
                .thenReturn(Optional.empty());

        assertThat(cache.getToken(999L)).isNull();
    }

    @Test
    @DisplayName("만료된 DB 토큰 조회 - null 반환")
    void getExpiredTokenFromDb() {
        BrokerToken expiredToken = BrokerToken.builder()
                .accountId(1L)
                .brokerType(BrokerType.KIS)
                .accessToken("encrypted-expired")
                .tokenType("Bearer")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.of(expiredToken));
        when(aesEncryptionUtil.decrypt("encrypted-expired")).thenReturn("expired-token");

        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("만료 5분 이내 토큰 조회 - null 반환 (갱신 유도)")
    void getAlmostExpiredToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "almost-expired", "Bearer", LocalDateTime.now().plusMinutes(3));

        when(aesEncryptionUtil.encrypt("almost-expired")).thenReturn("encrypted-almost");
        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.empty());

        cache.putToken(1L, token);

        // 메모리 캐시에 있지만 만료 5분 이내이므로 null
        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("토큰 삭제 - 메모리와 DB 모두 삭제")
    void removeToken() {
        BrokerTokenResponse token = new BrokerTokenResponse(
                "to-remove", "Bearer", LocalDateTime.now().plusHours(24));

        when(aesEncryptionUtil.encrypt("to-remove")).thenReturn("encrypted-remove");
        when(brokerTokenRepository.findByAccountIdAndBrokerType(eq(1L), eq(BrokerType.KIS)))
                .thenReturn(Optional.empty());

        cache.putToken(1L, token);
        cache.removeToken(1L);

        verify(brokerTokenRepository).deleteByAccountIdAndBrokerType(1L, BrokerType.KIS);

        // 메모리에서 제거 후 DB에서도 없으면 null
        when(brokerTokenRepository.findByAccountIdAndBrokerType(1L, BrokerType.KIS))
                .thenReturn(Optional.empty());
        assertThat(cache.getToken(1L)).isNull();
    }

    @Test
    @DisplayName("만료된 캐시 정리 - 메모리와 DB 모두 정리")
    void clearExpired() {
        when(brokerTokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(2);

        cache.clearExpired();

        verify(brokerTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}
