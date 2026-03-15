package com.stock.account.broker.kis;

import com.stock.account.broker.dto.BrokerTokenResponse;
import com.stock.account.common.util.AesEncryptionUtil;
import com.stock.account.domain.BrokerToken;
import com.stock.account.domain.BrokerType;
import com.stock.account.repository.BrokerTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KIS 토큰 캐시 (2-Layer: 메모리 + DB).
 * <p>
 * 메모리 캐시로 빠른 조회를 제공하고, DB에 영속화하여 서버 재시작 후에도
 * 기존 토큰을 재사용한다. KIS는 토큰 발급을 하루 1회만 권고하므로
 * 불필요한 재발급을 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenCache {

    private final BrokerTokenRepository brokerTokenRepository;
    private final AesEncryptionUtil aesEncryptionUtil;

    private final ConcurrentHashMap<Long, BrokerTokenResponse> memoryCache = new ConcurrentHashMap<>();

    /**
     * 캐시에서 유효한 토큰을 반환한다.
     * 1) 메모리 캐시 확인
     * 2) 메모리에 없으면 DB에서 조회 후 메모리에 적재
     * 만료 5분 전이면 null을 반환하여 갱신을 유도한다.
     */
    @Transactional(readOnly = true)
    public BrokerTokenResponse getToken(Long accountId) {
        // 1. 메모리 캐시 확인
        BrokerTokenResponse cached = memoryCache.get(accountId);
        if (cached != null) {
            if (isExpiringSoon(cached)) {
                memoryCache.remove(accountId);
                return null;
            }
            return cached;
        }

        // 2. DB에서 조회
        Optional<BrokerToken> dbToken = brokerTokenRepository
                .findByAccountIdAndBrokerType(accountId, BrokerType.KIS);

        if (dbToken.isEmpty()) {
            return null;
        }

        BrokerToken token = dbToken.get();
        BrokerTokenResponse response = toResponse(token);

        if (isExpiringSoon(response)) {
            return null;
        }

        // 메모리 캐시에 적재
        memoryCache.put(accountId, response);
        log.debug("DB에서 KIS 토큰 복원 - accountId: {}", accountId);
        return response;
    }

    /**
     * 토큰을 메모리 캐시와 DB에 동시에 저장한다.
     * REQUIRES_NEW: 호출자가 readOnly 트랜잭션이어도 독립 쓰기 트랜잭션을 사용한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void putToken(Long accountId, BrokerTokenResponse token) {
        // 메모리 캐시 저장
        memoryCache.put(accountId, token);

        // DB 저장 (upsert)
        String encryptedAccessToken = aesEncryptionUtil.encrypt(token.accessToken());
        Optional<BrokerToken> existing = brokerTokenRepository
                .findByAccountIdAndBrokerType(accountId, BrokerType.KIS);

        if (existing.isPresent()) {
            existing.get().updateToken(encryptedAccessToken, token.tokenType(), token.expiresAt());
        } else {
            BrokerToken entity = BrokerToken.builder()
                    .accountId(accountId)
                    .brokerType(BrokerType.KIS)
                    .accessToken(encryptedAccessToken)
                    .tokenType(token.tokenType())
                    .expiresAt(token.expiresAt())
                    .build();
            brokerTokenRepository.save(entity);
        }

        log.info("KIS 토큰 저장 완료 (메모리+DB) - accountId: {}", accountId);
    }

    /**
     * 토큰을 메모리 캐시와 DB에서 삭제한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeToken(Long accountId) {
        memoryCache.remove(accountId);
        brokerTokenRepository.deleteByAccountIdAndBrokerType(accountId, BrokerType.KIS);
    }

    /**
     * 만료된 토큰을 메모리 캐시와 DB에서 정리한다.
     * 매 1시간마다 자동 실행.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void clearExpired() {
        LocalDateTime now = LocalDateTime.now();
        memoryCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        int deleted = brokerTokenRepository.deleteExpiredTokens(now);
        if (deleted > 0) {
            log.info("만료된 토큰 {}건 정리 완료", deleted);
        }
    }

    private boolean isExpiringSoon(BrokerTokenResponse token) {
        return token.expiresAt().isBefore(LocalDateTime.now().plusMinutes(5));
    }

    private BrokerTokenResponse toResponse(BrokerToken entity) {
        String decryptedAccessToken = aesEncryptionUtil.decrypt(entity.getAccessToken());
        return new BrokerTokenResponse(
                decryptedAccessToken,
                entity.getTokenType(),
                entity.getExpiresAt()
        );
    }
}
