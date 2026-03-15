package com.stock.account.broker.dto;

import java.time.LocalDateTime;

public record BrokerTokenResponse(
        String accessToken,
        String tokenType,
        LocalDateTime expiresAt
) {
}
