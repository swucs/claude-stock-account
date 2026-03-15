package com.stock.account.dto;

import com.stock.account.common.util.MaskingUtil;
import com.stock.account.domain.Account;
import com.stock.account.domain.BrokerType;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        BrokerType brokerType,
        String brokerName,
        String accountNumber,
        String accountName,
        String appKey,
        String secretKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getBrokerType(),
                account.getBrokerType().getDisplayName(),
                MaskingUtil.maskAccountNumber(account.getAccountNumber()),
                account.getAccountName(),
                MaskingUtil.maskKey(account.getAppKey()),
                MaskingUtil.maskKey(account.getSecretKey()),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
