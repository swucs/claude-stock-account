package com.stock.account.broker.kiwoom;

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
 * 키움 토큰 캐시 (2-Layer: 메모리 + DB).
 * 메모리 캐시로 빠른 조회를 제공하고, DB에 영속화하여 서버 재시작 후에도 기존 토큰을 재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomTokenCache {

    private final BrokerTokenRepository brokerTokenRepository;
    private final AesEncryptionUtil aesEncryptionUtil;

    private final ConcurrentHashMap<Long, BrokerTokenResponse> memoryCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public BrokerTokenResponse getToken(Long accountId) {
        BrokerTokenResponse cached = memoryCache.get(accountId);
        if (cached != null) {
            if (isExpiringSoon(cached)) {
                memoryCache.remove(accountId);
                return null;
            }
            return cached;
        }

        Optional<BrokerToken> dbToken = brokerTokenRepository
                .findByAccountIdAndBrokerType(accountId, BrokerType.KIWOOM);

        if (dbToken.isEmpty()) {
            return null;
        }

        BrokerToken token = dbToken.get();
        BrokerTokenResponse response = toResponse(token);

        if (isExpiringSoon(response)) {
            return null;
        }

        memoryCache.put(accountId, response);
        log.debug("DB에서 키움 토큰 복원 - accountId: {}", accountId);
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void putToken(Long accountId, BrokerTokenResponse token) {
        memoryCache.put(accountId, token);

        String encryptedAccessToken = aesEncryptionUtil.encrypt(token.accessToken());
        Optional<BrokerToken> existing = brokerTokenRepository
                .findByAccountIdAndBrokerType(accountId, BrokerType.KIWOOM);

        if (existing.isPresent()) {
            existing.get().updateToken(encryptedAccessToken, token.tokenType(), token.expiresAt());
        } else {
            BrokerToken entity = BrokerToken.builder()
                    .accountId(accountId)
                    .brokerType(BrokerType.KIWOOM)
                    .accessToken(encryptedAccessToken)
                    .tokenType(token.tokenType())
                    .expiresAt(token.expiresAt())
                    .build();
            brokerTokenRepository.save(entity);
        }

        log.info("키움 토큰 저장 완료 (메모리+DB) - accountId: {}", accountId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeToken(Long accountId) {
        memoryCache.remove(accountId);
        brokerTokenRepository.deleteByAccountIdAndBrokerType(accountId, BrokerType.KIWOOM);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void clearExpired() {
        LocalDateTime now = LocalDateTime.now();
        memoryCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        int deleted = brokerTokenRepository.deleteExpiredTokens(now);
        if (deleted > 0) {
            log.info("만료된 키움 토큰 {}건 정리 완료", deleted);
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
