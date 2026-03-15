package com.stock.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "account",
        indexes = @Index(name = "idx_account_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BrokerType brokerType;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 100)
    private String accountName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String appKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String secretKey;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Account(User user, BrokerType brokerType, String accountNumber,
                   String accountName, String appKey, String secretKey, String additionalInfo) {
        this.user = user;
        this.brokerType = brokerType;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.appKey = appKey;
        this.secretKey = secretKey;
        this.additionalInfo = additionalInfo;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String accountNumber, String accountName, String appKey, String secretKey, String additionalInfo) {
        if (accountNumber != null) {
            this.accountNumber = accountNumber;
        }
        if (accountName != null) {
            this.accountName = accountName;
        }
        if (appKey != null) {
            this.appKey = appKey;
        }
        if (secretKey != null) {
            this.secretKey = secretKey;
        }
        if (additionalInfo != null) {
            this.additionalInfo = additionalInfo;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
