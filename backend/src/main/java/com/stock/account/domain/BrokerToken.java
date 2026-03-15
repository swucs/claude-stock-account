package com.stock.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 증권사 API 토큰을 DB에 영속화하는 엔티티.
 * KIS는 하루 1회 토큰 발급을 권고하므로 서버 재시작 시에도 기존 토큰을 재사용한다.
 */
@Entity
@Table(name = "broker_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_broker_token_account",
                columnNames = {"account_id", "broker_type"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrokerToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 20)
    private BrokerType brokerType;

    /** AES-256-GCM으로 암호화된 Access Token */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = false, length = 20)
    private String tokenType;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public BrokerToken(Long accountId, BrokerType brokerType,
                       String accessToken, String tokenType, LocalDateTime expiresAt) {
        this.accountId = accountId;
        this.brokerType = brokerType;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateToken(String accessToken, String tokenType, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }
}
